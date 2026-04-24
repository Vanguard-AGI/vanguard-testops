package io.vanguard.testops.workflow.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.metadata.domain.WorkflowEngineProfile;
import io.vanguard.testops.metadata.service.WorkflowEngineProfileService;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.domain.WorkflowDefinition;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import io.vanguard.testops.workflow.domain.WorkflowRunStep;
import io.vanguard.testops.workflow.domain.WorkflowRunLog;
import io.vanguard.testops.workflow.domain.WorkflowStep;
import io.vanguard.testops.workflow.domain.WorkflowStepLink;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.support.callback.WorkflowRunResultCallbackRequest;
import io.vanguard.testops.workflow.mapper.WorkflowDefinitionMapper;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import io.vanguard.testops.workflow.mapper.WorkflowTestReportMapper;
import io.vanguard.testops.workflow.domain.WorkflowTestReport;
import io.vanguard.testops.workflow.mapper.WorkflowRunStepMapper;
import io.vanguard.testops.workflow.service.WorkflowTestReportService;
import io.vanguard.testops.workflow.mapper.WorkflowRunLogMapper;
import io.vanguard.testops.workflow.mapper.WorkflowStepMapper;
import io.vanguard.testops.workflow.mapper.WorkflowStepLinkMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流运行服务
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowRunService {

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    @Resource
    private WorkflowRunStepMapper workflowRunStepMapper;

    @Resource
    private WorkflowRunLogMapper workflowRunLogMapper;

    @Resource
    private WorkflowTestReportMapper workflowTestReportMapper;

    @Resource
    private WorkflowTestReportService workflowTestReportService;

    @Resource
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Resource
    private WorkflowStepMapper workflowStepMapper;

    @Resource
    private WorkflowStepLinkMapper workflowStepLinkMapper;

    @Resource
    private WorkflowEngineProfileService workflowEngineProfileService;
    
    @Resource
    private io.vanguard.testops.metadata.mapper.WorkflowEngineProfileMapper workflowEngineProfileMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    // 执行机地址（从配置文件读取）
    @Value("${workflow.executor.base-url:http://192.168.29.107:8100}")
    private String executorBaseUrl;
    
    // 分批回调数据在Redis中的过期时间（秒），默认30分钟
    private static final long BATCH_CALLBACK_EXPIRE_SECONDS = 30 * 60;

    /**
     * 执行工作流（统一使用批量逻辑，单个执行时 workflowIds 只有一个元素）
     */
    public WorkflowRunDTO execute(WorkflowRunExecuteRequest request, String userId) {
        // 统一处理：优先使用 workflowIds，如果没有则使用 workflowId 转为列表
        List<String> workflowIdsToExecute;
        if (CollectionUtils.isNotEmpty(request.getWorkflowIds())) {
            workflowIdsToExecute = request.getWorkflowIds();
        } else if (StringUtils.isNotBlank(request.getWorkflowId())) {
            workflowIdsToExecute = List.of(request.getWorkflowId());
        } else {
            throw new MSException("工作流ID不能为空");
        }
        
        if (workflowIdsToExecute.isEmpty()) {
            throw new MSException("工作流ID列表不能为空");
        }
        
        // 1. 获取第一个工作流定义（用于获取项目ID、环境配置等公共信息）
        WorkflowDefinition firstDefinition = workflowDefinitionMapper.selectByWorkflowId(workflowIdsToExecute.get(0));
        if (firstDefinition == null) {
            throw new MSException("工作流不存在: " + workflowIdsToExecute.get(0));
        }

        // 2. 获取环境配置（用于设置 environment_name）
        // 优先从请求中获取，如果请求中没有，则从节点配置中提取
        WorkflowEngineProfile profile = null;
        String environmentId = request.getEnvironmentId();
        
        // 如果请求中没有环境ID，尝试从节点配置中提取（从第一个工作流提取）
        if (StringUtils.isBlank(environmentId)) {
            List<WorkflowStep> stepsForEnv = workflowStepMapper.selectByWorkflowId(workflowIdsToExecute.get(0));
            if (stepsForEnv != null && !stepsForEnv.isEmpty()) {
                for (WorkflowStep step : stepsForEnv) {
                    Map<String, Object> stepConfig = step.getStepConfig();
                    if (stepConfig != null) {
                        String nodeType = step.getStepType();
                        String extractedEnvId = null;
                        
                        // 根据节点类型获取环境ID（与调试接口逻辑一致）
                        if ("SQL".equals(nodeType) && stepConfig.get("connection") != null) {
                            Map<String, Object> connection = (Map<String, Object>) stepConfig.get("connection");
                            extractedEnvId = (String) connection.get("environmentId");
                        } else if ("XXL_JOB".equals(nodeType) || "XXLJOB".equals(nodeType)) {
                            extractedEnvId = (String) stepConfig.get("environmentId");
                        } else if ("MQ".equals(nodeType) || "ROCKETMQ".equals(nodeType)) {
                            extractedEnvId = (String) stepConfig.get("environmentId");
                        } else if ("HTTP".equals(nodeType) || "API".equals(nodeType) || "HTTP_REQUEST".equals(nodeType)) {
                            // 如果节点配置中有 config 字段，从 config 中获取
                            if (stepConfig.containsKey("config")) {
                                Map<String, Object> config = (Map<String, Object>) stepConfig.get("config");
                                extractedEnvId = (String) config.get("environmentId");
                            } else {
                                // 否则直接从 stepConfig 中获取
                                extractedEnvId = (String) stepConfig.get("environmentId");
                            }
                        }
                        
                        if (StringUtils.isNotBlank(extractedEnvId)) {
                            environmentId = extractedEnvId;
                            log.debug("从节点配置中提取到环境ID: {} (节点类型: {}, 节点ID: {})", environmentId, nodeType, step.getStepId());
                            break; // 找到第一个环境ID就停止
                        }
                    }
                }
            }
        }
        
        // 获取环境配置
        if (StringUtils.isNotBlank(environmentId)) {
            try {
                profile = workflowEngineProfileMapper.selectByIdWithTimestamp(environmentId);
                log.debug("成功获取环境配置: environmentId={}, environmentName={}", environmentId, profile != null ? profile.getName() : null);
            } catch (Exception e) {
                log.warn("获取环境配置失败: environmentId={}, error={}", environmentId, e.getMessage());
            }
        } else {
            log.debug("未找到环境ID（请求中未提供，节点配置中也未找到）");
        }

        // 3. 生成 reportId 和创建报告（统一逻辑，单个和批量都走这个）
        long currentTime = System.currentTimeMillis();
        String reportId = IDGenerator.nextStr();
        String triggerType = StringUtils.defaultIfBlank(request.getTriggerType(), "MANUAL");
        String environmentName = profile != null ? profile.getName() : null;
        
        // 创建测试报告（统一逻辑）
        WorkflowTestReport testReport = new WorkflowTestReport();
        testReport.setReportId(reportId);
        testReport.setProjectId(firstDefinition.getProjectId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String defaultReportName = "RPT-" + sdf.format(new Date(currentTime));
        testReport.setReportName(defaultReportName);
        testReport.setReportType(mapTriggerTypeToReportType(triggerType));
        testReport.setExecutor(userId);
        testReport.setTriggerType(triggerType);
        testReport.setStatus("RUNNING");
        testReport.setStartTime(currentTime);
        testReport.setTotalWorkflows(workflowIdsToExecute.size());
        testReport.setTotalTests(0);
        testReport.setSuccessTests(0);
        testReport.setFailedTests(0);
        testReport.setSkippedTests(0);
        testReport.setPendingTests(0);
        testReport.setEnvironmentId(environmentId);
        testReport.setEnvironmentName(environmentName);
        testReport.setCreateTime(currentTime);
        testReport.setCreateUser(userId);
        testReport.setUpdateTime(currentTime);
        testReport.setUpdateUser(userId);
        
        // 初始化结果摘要
        Map<String, Object> resultSummary = new HashMap<>();
        resultSummary.put("workflowIds", workflowIdsToExecute);
        resultSummary.put("workflowStatus", new HashMap<>());
        testReport.setResultSummary(resultSummary);
        
        workflowTestReportMapper.insert(testReport);
        
        // 4. 为每个 workflow 创建运行记录，并组装 workflows 数组
        List<WorkflowRun> runs = new ArrayList<>();
        List<Map<String, Object>> workflows = new ArrayList<>();
        
        for (String workflowId : workflowIdsToExecute) {
            // 获取工作流定义
            WorkflowDefinition definition = workflowDefinitionMapper.selectByWorkflowId(workflowId);
            if (definition == null) {
                log.warn("工作流不存在，跳过: {}", workflowId);
                continue;
            }
            
            // 创建工作流运行记录
            WorkflowRun run = new WorkflowRun();
            run.setRunId(IDGenerator.nextStr());
            run.setReportId(reportId); // 所有 workflow 使用同一个 reportId
            run.setWorkflowId(workflowId);
            run.setProjectId(definition.getProjectId());
            run.setModuleId(definition.getModuleId());
            run.setWorkflowName(definition.getName());
            run.setTriggerType(triggerType);
            run.setTriggerUser(userId);
            run.setStatus("PENDING");
            run.setEnvironmentId(environmentId);
            if (profile != null && StringUtils.isNotBlank(profile.getName())) {
                run.setEnvironmentName(profile.getName());
            }
            run.setCreateTime(currentTime);
            run.setUpdateTime(currentTime);
            workflowRunMapper.insert(run);
            runs.add(run);
            
            // 组装 workflow 数据
            Map<String, Object> workflowItem = buildWorkflowItem(workflowId, definition, profile, run.getRunId(), request.getUserVariables());
            workflows.add(workflowItem);
        }
        
        if (runs.isEmpty()) {
            throw new MSException("没有可执行的工作流");
        }
        
        log.info("批量执行: reportId={}, workflowCount={}", reportId, runs.size());

        // 5. 组装数据并调用执行机
        try {
            // 组装执行机请求数据（执行机期望的格式）
            Map<String, Object> executorRequest = new HashMap<>();
            // reportId：生成后传递给执行机，执行机执行完毕后会在数据库写入
            executorRequest.put("reportId", reportId);
            executorRequest.put("priority", "normal");
            executorRequest.put("maxBatchSize", 1000);
            
            // 使用已经组装好的 workflows 数组
            executorRequest.put("workflows", workflows);

            // 注意：environment 配置不传递给执行机，环境变量已合并到 globalVariables 中

            // 调用执行机接口
            String executorUrl = executorBaseUrl + "/workflow/execute/batch";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(executorRequest, headers);
            
            log.info("调用执行机接口: {}", executorUrl);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(executorUrl, HttpMethod.POST, httpEntity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            // 更新所有运行记录的状态
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if (Boolean.TRUE.equals(result.get("success"))) {
                    for (WorkflowRun r : runs) {
                        r.setStatus("RUNNING");
                        r.setUpdateTime(System.currentTimeMillis());
                        workflowRunMapper.updateById(r);
                    }
                } else {
                    for (WorkflowRun r : runs) {
                        r.setStatus("FAILED");
                        r.setUpdateTime(System.currentTimeMillis());
                        workflowRunMapper.updateById(r);
                    }
                }
            } else {
                for (WorkflowRun r : runs) {
                    r.setStatus("FAILED");
                    r.setUpdateTime(System.currentTimeMillis());
                    workflowRunMapper.updateById(r);
                }
            }
        } catch (Exception e) {
            log.error("调用执行机失败: {}", e.getMessage(), e);
            for (WorkflowRun r : runs) {
                r.setStatus("FAILED");
                r.setUpdateTime(System.currentTimeMillis());
                workflowRunMapper.updateById(r);
            }
        }

        // 6. 返回第一个运行记录（用于兼容单个执行）
        return convertToDTO(runs.get(0));
    }

    /**
     * 调试单个节点
     * 注意：调试模式不创建运行记录，也不传递reportId
     */
    public Map<String, Object> debugNode(WorkflowDebugNodeRequest request, String userId) {
        // 1. 获取工作流定义（用于获取全局变量等）
        WorkflowDefinition definition = workflowDefinitionMapper.selectByWorkflowId(request.getWorkflowId());
        if (definition == null) {
            throw new MSException("工作流不存在");
        }

        // 2. 查找节点（从 WorkflowStep 表中获取）
        List<WorkflowStep> steps = workflowStepMapper.selectByWorkflowId(request.getWorkflowId());
        WorkflowStep targetStep = null;
        if (steps != null) {
            for (WorkflowStep step : steps) {
                if (request.getNodeId().equals(step.getStepId())) {
                    targetStep = step;
                    break;
                }
            }
        }
        
        if (targetStep == null) {
            throw new MSException("节点不存在");
        }

        // 3. 获取节点配置（优先使用请求中的配置，否则使用数据库中的配置）
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeConfig = request.getNodeConfig() != null 
            ? (Map<String, Object>) request.getNodeConfig() 
            : targetStep.getStepConfig();
        
        if (nodeConfig == null) {
            nodeConfig = new HashMap<>();
        }

        // 4. 获取环境配置
        WorkflowEngineProfile profile = null;
        String originalNodeType = targetStep.getStepType();
        String environmentId = null;
        
        // 优先从 nodeConfig 顶层获取 environmentId（前端统一传递方式）
        if (nodeConfig.containsKey("environmentId")) {
            environmentId = (String) nodeConfig.get("environmentId");
        } else {
            // 兼容旧逻辑：从嵌套位置获取（向后兼容）
            if ("SQL".equals(originalNodeType) || "MYSQL".equals(originalNodeType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> connection = (Map<String, Object>) nodeConfig.get("connection");
                if (connection != null) {
                    environmentId = (String) connection.get("environmentId");
                }
            } else if ("XXL_JOB".equals(originalNodeType) || "XXLJOB".equals(originalNodeType)) {
                environmentId = (String) nodeConfig.get("environmentId");
            } else if ("MQ".equals(originalNodeType) || "ROCKETMQ".equals(originalNodeType)) {
                environmentId = (String) nodeConfig.get("environmentId");
            } else if ("HTTP".equals(originalNodeType) || "API".equals(originalNodeType) || "HTTP_REQUEST".equals(originalNodeType)) {
                environmentId = (String) nodeConfig.get("environmentId");
            }
        }

        // 获取环境配置
        if (StringUtils.isNotBlank(environmentId)) {
            try {
                profile = workflowEngineProfileMapper.selectByIdWithTimestamp(environmentId);
            } catch (Exception e) {
                log.warn("获取环境配置失败: {}", e.getMessage());
            }
        }

        // 5. 创建调试运行记录（调试模式也需要创建运行记录，以便接收回调）
        long currentTime = System.currentTimeMillis();
        WorkflowRun debugRun = new WorkflowRun();
        String debugRunId = IDGenerator.nextStr();
        debugRun.setRunId(debugRunId);
        debugRun.setWorkflowId(request.getWorkflowId());
        debugRun.setProjectId(definition.getProjectId());
        debugRun.setModuleId(definition.getModuleId());
        debugRun.setWorkflowName(definition.getName());
        debugRun.setTriggerType("DEBUG");
        debugRun.setTriggerUser(userId);
        debugRun.setStatus("PENDING");
        debugRun.setEnvironmentId(environmentId);
        // 设置环境名称（执行机回调时不会提供，需要在创建时设置）
        if (profile != null && StringUtils.isNotBlank(profile.getName())) {
            debugRun.setEnvironmentName(profile.getName());
        }
        debugRun.setCreateTime(currentTime);
        debugRun.setUpdateTime(currentTime);
        workflowRunMapper.insert(debugRun);
        
        // 6. 组装执行机请求数据（调试模式：只生成 runId，不生成 reportId）
        Map<String, Object> executorRequest = new HashMap<>();
        executorRequest.put("runId", debugRunId);
        // 调试模式不生成 reportId
        
        // 转换节点类型（执行机期望小写加下划线格式）
        String nodeType = convertNodeTypeForExecutor(originalNodeType);
        
        // 节点数据：执行机期望格式 {id, stepName, type, data: {config, assertion, extractions}}
        Map<String, Object> node = new HashMap<>();
        node.put("id", targetStep.getStepId());
        node.put("stepName", targetStep.getName()); // 添加节点名称，供执行机回调时使用
        node.put("type", nodeType);
        
        // 节点数据：包含 config, assertion, extractions
        Map<String, Object> nodeData = new HashMap<>();
        
        // config：从 nodeConfig 中提取，如果不存在则使用整个 nodeConfig
        Map<String, Object> config;
        if (nodeConfig.containsKey("config")) {
            config = new HashMap<>((Map<String, Object>) nodeConfig.get("config"));
        } else {
            // 如果没有 config 字段，将整个 nodeConfig 作为 config（排除 assertion 和 extractions）
            config = new HashMap<>(nodeConfig);
            config.remove("assertion");
            config.remove("extractions");
        }
        
        // HTTP_REQUEST 节点特殊处理：根据 paramType 只保留对应字段（需要在processNodeConfig之前处理）
        if ("http_request".equals(nodeType)) {
            String paramType = (String) config.get("paramType");
            if (StringUtils.isNotBlank(paramType)) {
                // 先备份一份原始配置，用于从中恢复各个字段
                Map<String, Object> originalConfig;
                if (nodeConfig.containsKey("config")) {
                    originalConfig = (Map<String, Object>) nodeConfig.get("config");
                } else {
                    originalConfig = nodeConfig;
                }
                
                // 移除所有参数类型字段
                config.remove("params");
                config.remove("json");
                config.remove("data");
                config.remove("upload");
                config.remove("body");
                
                if ("upload".equals(paramType)) {
                    // upload 模式：需要同时保留文件字段(upload)和表单字段(data)
                    if (originalConfig.containsKey("upload")) {
                        config.put("upload", originalConfig.get("upload"));
                    }
                    if (originalConfig.containsKey("data")) {
                        config.put("data", originalConfig.get("data"));
                    }
                } else {
                    // 其他模式：保持旧逻辑，只恢复对应一种字段
                    if (originalConfig.containsKey(paramType)) {
                        config.put(paramType, originalConfig.get(paramType));
                    }
                }
            }
        }
        
        // 获取用户输入的变量（从请求中获取）
        Map<String, String> userVariables = request.getUserVariables();
        if (userVariables == null) {
            userVariables = new HashMap<>();
        }
        
        // 使用统一的节点配置处理方法（传递 userVariables 以便处理 headers）
        NodeConfigProcessResult processResult = processNodeConfig(config, nodeType, userVariables, profile);
        config = processResult.config;
        Map<String, Object> nodeEnvironmentVariables = processResult.environmentVariables;
        
        nodeData.put("config", config);
        
        // assertion：从 nodeConfig 中提取，如果不存在则为空对象
        if (nodeConfig.containsKey("assertion")) {
            nodeData.put("assertion", nodeConfig.get("assertion"));
        } else {
            nodeData.put("assertion", new HashMap<>());
        }
        
        // extractions：从 nodeConfig 中提取，如果不存在则为空数组
        if (nodeConfig.containsKey("extractions")) {
            nodeData.put("extractions", nodeConfig.get("extractions"));
        } else {
            nodeData.put("extractions", new ArrayList<>());
        }
        
        node.put("data", nodeData);
        
        // 工作流对象（调试模式只包含单个节点）
        Map<String, Object> workflow = new HashMap<>();
        workflow.put("workflowId", request.getWorkflowId());
        workflow.put("workflowName", definition.getName());
        workflow.put("nodes", java.util.Collections.singletonList(node));
        // edges 数组始终存在，即使为空（调试模式只有一个节点，没有连线）
        workflow.put("edges", new ArrayList<>());
        executorRequest.put("workflow", workflow);

        // 全局变量（执行机期望的格式）
        Map<String, Object> globalVariables = new HashMap<>();
        
        // 先添加工作流定义中的全局变量
        if (definition.getGlobalVars() != null && !definition.getGlobalVars().isEmpty()) {
            for (Map.Entry<String, Object> entry : definition.getGlobalVars().entrySet()) {
                Object processedValue = processVariableValue(entry.getKey(), entry.getValue());
                globalVariables.put(entry.getKey(), processedValue);
            }
            log.debug("调试模式 - 添加工作流全局变量: {}", definition.getGlobalVars());
        } else {
            log.debug("调试模式 - 工作流定义中的全局变量为空或null");
        }
        
        // 将环境变量合并到 globalVariables 中
        if (profile != null) {
            if (profile.getVariables() != null && !profile.getVariables().isEmpty()) {
                for (Map.Entry<String, Object> entry : profile.getVariables().entrySet()) {
                    Object processedValue = processVariableValue(entry.getKey(), entry.getValue());
                    globalVariables.put(entry.getKey(), processedValue);
                }
                log.debug("调试模式 - 添加环境变量: {}", profile.getVariables());
            } else {
                log.debug("调试模式 - 环境配置中的变量为空或null, environmentId: {}", environmentId);
            }
            
            // 将环境配置的结构化数据扁平化到 globalVariables 中（不再使用嵌套结构）
            // 1. HTTP节点：domain -> globalVariables.url（扁平化）
            if (StringUtils.isNotBlank(profile.getDomain())) {
                if (!globalVariables.containsKey("url")) {
                    globalVariables.put("url", profile.getDomain());
                    log.debug("调试模式 - 添加环境配置 url: {}", profile.getDomain());
                }
            }
            
            // 2. SQL节点：dataEndpoint 扁平化到 globalVariables（字段重命名：host->data_host, port->data_port, user->data_user, password->data_password）
            if (profile.getDataEndpoint() != null && !profile.getDataEndpoint().isEmpty()) {
                Map<String, Object> dataEndpoint = profile.getDataEndpoint();
                
                Object hostValue = dataEndpoint.get("data_host");
                if (hostValue != null && !globalVariables.containsKey("data_host")) {
                    Object processedValue = processVariableValue("data_host", hostValue);
                    globalVariables.put("data_host", processedValue);
                    log.debug("调试模式 - 添加环境配置 data_host: {}", hostValue);
                }
                
                Object portValue = dataEndpoint.get("data_port");
                if (portValue != null && !globalVariables.containsKey("data_port")) {
                    Object processedValue = processVariableValue("data_port", portValue);
                    globalVariables.put("data_port", processedValue);
                    log.debug("调试模式 - 添加环境配置 data_port: {}", portValue);
                }
                
                Object userValue = dataEndpoint.get("data_user");
                if (userValue != null && !globalVariables.containsKey("data_user")) {
                    Object processedValue = processVariableValue("data_user", userValue);
                    globalVariables.put("data_user", processedValue);
                    log.debug("调试模式 - 添加环境配置 data_user: {}", userValue);
                }
                
                Object passwordValue = dataEndpoint.get("data_password");
                if (passwordValue != null && !globalVariables.containsKey("data_password")) {
                    Object processedValue = processVariableValue("data_password", passwordValue);
                    globalVariables.put("data_password", processedValue);
                    log.debug("调试模式 - 添加环境配置 data_password: {}", passwordValue);
                }
            }
            
            // 3. MQ节点：mqInfo.mq_url -> globalVariables.mq_url（扁平化）
            if (profile.getMqInfo() != null && !profile.getMqInfo().isEmpty()) {
                Object mqUrl = profile.getMqInfo().get("mq_url");
                if (mqUrl != null && !globalVariables.containsKey("mq_url")) {
                    Object processedValue = processVariableValue("mq_url", mqUrl);
                    globalVariables.put("mq_url", processedValue);
                    log.debug("调试模式 - 添加环境配置 mq_url: {}", mqUrl);
                }
            }
            
            // 4. Dubbo节点：dubboInfo.dubbo_url -> globalVariables.dubbo_url（扁平化）
            if (profile.getDubboInfo() != null && !profile.getDubboInfo().isEmpty()) {
                Object dubboUrl = profile.getDubboInfo().get("dubbo_url");
                if (dubboUrl != null && !globalVariables.containsKey("dubbo_url")) {
                    Object processedValue = processVariableValue("dubbo_url", dubboUrl);
                    globalVariables.put("dubbo_url", processedValue);
                    log.debug("调试模式 - 添加环境配置 dubbo_url: {}", dubboUrl);
                }
            }
            
            // 5. XXL-Job节点：xxljobInfo 扁平化到 globalVariables（字段重命名：user->xxljobuser, password->xxljobpassword, xxjob_url保持不变）
            if (profile.getXxljobInfo() != null && !profile.getXxljobInfo().isEmpty()) {
                Map<String, Object> xxljobInfo = profile.getXxljobInfo();
                
                Object xxjobUrlValue = xxljobInfo.get("xxjob_url");
                if (xxjobUrlValue != null && !globalVariables.containsKey("xxjob_url")) {
                    Object processedValue = processVariableValue("xxjob_url", xxjobUrlValue);
                    globalVariables.put("xxjob_url", processedValue);
                    log.debug("调试模式 - 添加环境配置 xxjob_url: {}", xxjobUrlValue);
                }
                
                Object userValue = xxljobInfo.get("xxljobuser");
                if (userValue != null && !globalVariables.containsKey("xxljobuser")) {
                    Object processedValue = processVariableValue("xxljobuser", userValue);
                    globalVariables.put("xxljobuser", processedValue);
                    log.debug("调试模式 - 添加环境配置 xxljobuser: {}", userValue);
                }
                
                Object passwordValue = xxljobInfo.get("xxljobpassword");
                if (passwordValue != null && !globalVariables.containsKey("xxljobpassword")) {
                    Object processedValue = processVariableValue("xxljobpassword", passwordValue);
                    globalVariables.put("xxljobpassword", processedValue);
                    log.debug("调试模式 - 添加环境配置 xxljobpassword: {}", passwordValue);
                }
            }
        } else {
            log.debug("调试模式 - 未获取到环境配置, environmentId: {}", environmentId);
        }
        
        // 注意：不再使用嵌套结构，所有变量都扁平化到 globalVariables 中
        // 节点的 environmentVariables 已经在 processNodeConfig 中处理，这里不再需要额外合并
        
        // 最后添加用户输入的变量（优先级最高，覆盖环境变量和节点environmentVariables）
        if (userVariables != null && !userVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : userVariables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (StringUtils.isNotBlank(value)) {
                    // 检查globalVariables中是否已有相同key（不区分大小写），如果有则替换
                    String existingKey = null;
                    for (String varKey : globalVariables.keySet()) {
                        if (key.equalsIgnoreCase(varKey)) {
                            existingKey = varKey;
                            break;
                        }
                    }
                    if (existingKey != null) {
                        globalVariables.remove(existingKey);
                    }
                    Object processedValue = processVariableValue(key, value);
                    globalVariables.put(key, processedValue);
                    log.debug("调试模式 - 添加用户输入的变量: {} = {}", key, value);
                }
            }
        }
        
        log.info("调试模式 - 最终 globalVariables: {}", globalVariables);
        executorRequest.put("globalVariables", globalVariables);

        // 注意：environment 配置不传递给执行机，环境变量已合并到 globalVariables 中

        // 7. 调用执行机接口（调试模式使用调试接口）
        try {
            // 调试节点调用执行机调试接口
            String executorUrl = executorBaseUrl + "/workflow/debug/execute";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(executorRequest, headers);
            
            log.info("调用执行机接口（调试节点）: url={}, workflowId={}, nodeId={}, nodeType={}", 
                    executorUrl, request.getWorkflowId(), request.getNodeId(), originalNodeType);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(executorUrl, HttpMethod.POST, httpEntity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = new HashMap<>(response.getBody());
                // 添加 runId，用于前端获取执行详情
                result.put("runId", debugRunId);
                return result;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "执行机返回错误");
                errorResult.put("runId", debugRunId); // 即使失败也返回 runId
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用执行机失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "调用执行机失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 调试公共节点（无工作流上下文，仅按 projectId + nodeId + nodeConfig 执行，返回 runId 格式与 debugNode 一致）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> debugPublicNode(WorkflowDebugPublicNodeRequest request, String userId) {
        Map<String, Object> nodeConfig = request.getNodeConfig() != null
            ? (Map<String, Object>) request.getNodeConfig()
            : new HashMap<>();

        String originalNodeType = StringUtils.isNotBlank(request.getNodeType())
            ? request.getNodeType()
            : inferNodeTypeFromConfig(nodeConfig);
        String nodeName = StringUtils.isNotBlank(request.getNodeName()) ? request.getNodeName() : "公共节点";

        String environmentId = null;
        if (nodeConfig.containsKey("environmentId")) {
            environmentId = (String) nodeConfig.get("environmentId");
        }
        WorkflowEngineProfile profile = null;
        if (StringUtils.isNotBlank(environmentId)) {
            try {
                profile = workflowEngineProfileMapper.selectByIdWithTimestamp(environmentId);
            } catch (Exception e) {
                log.warn("获取环境配置失败: {}", e.getMessage());
            }
        }

        long currentTime = System.currentTimeMillis();
        WorkflowRun debugRun = new WorkflowRun();
        String debugRunId = IDGenerator.nextStr();
        debugRun.setRunId(debugRunId);
        debugRun.setWorkflowId(request.getNodeId());
        debugRun.setProjectId(request.getProjectId());
        debugRun.setModuleId(null);
        debugRun.setWorkflowName(nodeName);
        debugRun.setTriggerType("DEBUG");
        debugRun.setTriggerUser(userId);
        debugRun.setStatus("PENDING");
        debugRun.setEnvironmentId(environmentId);
        if (profile != null && StringUtils.isNotBlank(profile.getName())) {
            debugRun.setEnvironmentName(profile.getName());
        }
        debugRun.setCreateTime(currentTime);
        debugRun.setUpdateTime(currentTime);
        workflowRunMapper.insert(debugRun);

        Map<String, Object> executorRequest = new HashMap<>();
        executorRequest.put("runId", debugRunId);
        String nodeType = convertNodeTypeForExecutor(originalNodeType);

        Map<String, Object> node = new HashMap<>();
        node.put("id", request.getNodeId());
        node.put("stepName", nodeName);
        node.put("type", nodeType);

        Map<String, Object> config;
        if (nodeConfig.containsKey("config")) {
            config = new HashMap<>((Map<String, Object>) nodeConfig.get("config"));
        } else {
            config = new HashMap<>(nodeConfig);
            config.remove("assertion");
            config.remove("extractions");
        }
        if ("http_request".equals(nodeType)) {
            String paramType = (String) config.get("paramType");
            if (StringUtils.isNotBlank(paramType)) {
                config.remove("params");
                config.remove("json");
                config.remove("data");
                config.remove("upload");
                config.remove("body");
                if (nodeConfig.containsKey(paramType)) {
                    config.put(paramType, nodeConfig.get(paramType));
                }
            }
        }

        Map<String, String> userVariables = request.getUserVariables();
        if (userVariables == null) {
            userVariables = new HashMap<>();
        }
        NodeConfigProcessResult processResult = processNodeConfig(config, nodeType, userVariables, profile);
        config = processResult.config;

        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("config", config);
        nodeData.put("assertion", nodeConfig.containsKey("assertion") ? nodeConfig.get("assertion") : new HashMap<>());
        nodeData.put("extractions", nodeConfig.containsKey("extractions") ? nodeConfig.get("extractions") : new ArrayList<>());
        node.put("data", nodeData);

        Map<String, Object> workflow = new HashMap<>();
        workflow.put("workflowId", request.getNodeId());
        workflow.put("workflowName", nodeName);
        workflow.put("nodes", java.util.Collections.singletonList(node));
        workflow.put("edges", new ArrayList<>());
        executorRequest.put("workflow", workflow);

        Map<String, Object> globalVariables = new HashMap<>();
        if (profile != null && profile.getVariables() != null && !profile.getVariables().isEmpty()) {
            for (Map.Entry<String, Object> entry : profile.getVariables().entrySet()) {
                Object processedValue = processVariableValue(entry.getKey(), entry.getValue());
                globalVariables.put(entry.getKey(), processedValue);
            }
        }
        if (profile != null) {
            if (StringUtils.isNotBlank(profile.getDomain()) && !globalVariables.containsKey("url")) {
                globalVariables.put("url", profile.getDomain());
            }
            if (profile.getDataEndpoint() != null && !profile.getDataEndpoint().isEmpty()) {
                Map<String, Object> dataEndpoint = profile.getDataEndpoint();
                putIfAbsent(globalVariables, "data_host", dataEndpoint.get("data_host"));
                putIfAbsent(globalVariables, "data_port", dataEndpoint.get("data_port"));
                putIfAbsent(globalVariables, "data_user", dataEndpoint.get("data_user"));
                putIfAbsent(globalVariables, "data_password", dataEndpoint.get("data_password"));
            }
            if (profile.getMqInfo() != null && profile.getMqInfo().get("mq_url") != null && !globalVariables.containsKey("mq_url")) {
                globalVariables.put("mq_url", processVariableValue("mq_url", profile.getMqInfo().get("mq_url")));
            }
            if (profile.getDubboInfo() != null && profile.getDubboInfo().get("dubbo_url") != null && !globalVariables.containsKey("dubbo_url")) {
                globalVariables.put("dubbo_url", processVariableValue("dubbo_url", profile.getDubboInfo().get("dubbo_url")));
            }
            if (profile.getXxljobInfo() != null && !profile.getXxljobInfo().isEmpty()) {
                Map<String, Object> xxl = profile.getXxljobInfo();
                putIfAbsent(globalVariables, "xxjob_url", xxl.get("xxjob_url"));
                putIfAbsent(globalVariables, "xxljobuser", xxl.get("xxljobuser"));
                putIfAbsent(globalVariables, "xxljobpassword", xxl.get("xxljobpassword"));
            }
        }
        if (userVariables != null && !userVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : userVariables.entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    String key = entry.getKey();
                    String existingKey = null;
                    for (String varKey : globalVariables.keySet()) {
                        if (key.equalsIgnoreCase(varKey)) {
                            existingKey = varKey;
                            break;
                        }
                    }
                    if (existingKey != null) {
                        globalVariables.remove(existingKey);
                    }
                    globalVariables.put(key, processVariableValue(key, entry.getValue()));
                }
            }
        }
        executorRequest.put("globalVariables", globalVariables);

        try {
            String executorUrl = executorBaseUrl + "/workflow/debug/execute";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(executorRequest, headers);
            log.info("调用执行机接口（调试公共节点）: url={}, nodeId={}, nodeType={}", executorUrl, request.getNodeId(), originalNodeType);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(executorUrl, HttpMethod.POST, httpEntity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = new HashMap<>(response.getBody());
                result.put("runId", debugRunId);
                return result;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "执行机返回错误");
                errorResult.put("runId", debugRunId);
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用执行机失败（公共节点）: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "调用执行机失败: " + e.getMessage());
            return errorResult;
        }
    }

    private static void putIfAbsent(Map<String, Object> map, String key, Object value) {
        if (value != null && !map.containsKey(key)) {
            map.put(key, value);
        }
    }

    private String inferNodeTypeFromConfig(Map<String, Object> nodeConfig) {
        if (nodeConfig.containsKey("method") && (nodeConfig.containsKey("url") || nodeConfig.containsKey("path"))) {
            return "HTTP_REQUEST";
        }
        if (nodeConfig.containsKey("sql")) {
            return "MYSQL";
        }
        if (nodeConfig.containsKey("interfaceName") && nodeConfig.containsKey("methodName")) {
            return "DUBBO";
        }
        if (nodeConfig.containsKey("mq_url") || (nodeConfig.containsKey("topic") && nodeConfig.containsKey("group"))) {
            return "ROCKETMQ";
        }
        if (nodeConfig.containsKey("condition")) {
            return "CONDITION";
        }
        if (nodeConfig.containsKey("loopType") || nodeConfig.containsKey("sub_nodes")) {
            return "LOOP";
        }
        if (nodeConfig.containsKey("script") || nodeConfig.containsKey("code")) {
            return "SCRIPT";
        }
        if (nodeConfig.containsKey("xxjob_url") || nodeConfig.containsKey("jobId")) {
            return "XXL_JOB";
        }
        return "HTTP_REQUEST";
    }

    /**
     * 获取运行详情
     */
    public WorkflowRunDTO getRunDetail(String runId) {
        WorkflowRun run = workflowRunMapper.selectByRunId(runId);
        if (run == null) {
            throw new MSException("运行记录不存在");
        }

        WorkflowRunDTO dto = convertToDTO(run);

        // 加载步骤执行明细
        List<WorkflowRunStep> steps = workflowRunStepMapper.selectByRunId(runId);
        if (CollectionUtils.isNotEmpty(steps)) {
            List<WorkflowRunStepDTO> stepDTOs = steps.stream()
                    .map(this::convertStepToDTO)
                    .collect(Collectors.toList());
            dto.setSteps(stepDTOs);
        }

        return dto;
    }

    /**
     * 分页查询运行记录
     * 查询逻辑：
     * - 当只传入 triggerUser（不传入 projectId）时，包含 DEBUG 类型的记录（用于查询调试历史）
     * - 当同时传入 triggerUser 和 projectId 时，排除 DEBUG 类型的记录（用于查询执行历史，按用户和项目过滤）
     * - 当不传入 triggerUser 时，排除 DEBUG 类型的记录（用于查询执行历史）
     */
    public Pager<List<WorkflowRunDTO>> getRunList(WorkflowRunPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");

        // 查询运行记录
        List<WorkflowRun> runs = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getWorkflowId())) {
            // 如果传入了projectId或triggerUser，使用带过滤条件的查询
            if (StringUtils.isNotBlank(request.getProjectId()) || StringUtils.isNotBlank(request.getTriggerUser())) {
                // selectByWorkflowIdAndFilters 会根据传入的参数来决定是否包含 DEBUG 类型：
                // - 只传入 triggerUser：包含 DEBUG 类型（调试历史）
                // - 同时传入 triggerUser 和 projectId：排除 DEBUG 类型（执行历史）
                runs = workflowRunMapper.selectByWorkflowIdAndFilters(
                    request.getWorkflowId(),
                    request.getProjectId(),
                    request.getTriggerUser()
                );
            } else {
                // 否则使用简单的按工作流ID查询（排除DEBUG类型）
            runs = workflowRunMapper.selectByWorkflowId(request.getWorkflowId());
            }
        } else if (StringUtils.isNotBlank(request.getProjectId())) {
            // 按项目ID查询（排除DEBUG类型）
            runs = workflowRunMapper.selectByProjectId(request.getProjectId());
        }

        List<WorkflowRunDTO> dtoList = runs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageUtils.setPageInfo(page, dtoList);
    }

    /**
     * 取消执行
     */
    public void cancel(String runId, String userId) {
        WorkflowRun run = workflowRunMapper.selectByRunId(runId);
        if (run == null) {
            throw new MSException("运行记录不存在");
        }

        if (!"RUNNING".equals(run.getStatus()) && !"PENDING".equals(run.getStatus())) {
            throw new MSException("只有运行中或待执行的工作流可以取消");
        }

        long currentTime = System.currentTimeMillis();
        run.setStatus("CANCELLED");
        if (run.getEndTime() == null) {
            run.setEndTime(currentTime);
        }
        if (run.getDurationMs() == null) {
            long startAt = run.getStartTime() != null ? run.getStartTime() :
                (run.getCreateTime() != null ? run.getCreateTime() : currentTime);
            run.setDurationMs(Math.max(0L, currentTime - startAt));
        }
        run.setUpdateTime(currentTime);
        workflowRunMapper.updateById(run);

        if (StringUtils.isNotBlank(run.getReportId())) {
            try {
                workflowTestReportService.updateReportOnWorkflowComplete(run.getReportId());
            } catch (Exception e) {
                log.error("取消工作流后更新测试报告失败: reportId={}, runId={}, error={}",
                    run.getReportId(), runId, e.getMessage(), e);
            }
        }

        try {
            WorkflowRunDTO runDetail = getRunDetail(runId);
            List<Map<String, Object>> stepsData = buildWorkflowStepsData(runDetail);
            io.vanguard.testops.workflow.support.socket.WorkflowWebSocketHandler.sendWorkflowStatusUpdate(
                runId,
                runDetail.getStatus(),
                "工作流已取消",
                runDetail.getTotalSteps(),
                runDetail.getPassedCount(),
                runDetail.getFailedCount()
            );
            io.vanguard.testops.workflow.support.socket.WorkflowWebSocketHandler.sendWorkflowResult(
                runId,
                runDetail.getStatus(),
                "工作流已取消",
                stepsData,
                null
            );
        } catch (Exception e) {
            log.error("取消工作流后推送状态失败: runId={}, error={}", runId, e.getMessage(), e);
        }
    }

    /**
     * 删除运行记录（软删除）
     */
    public void delete(String runId, String userId) {
        WorkflowRun run = workflowRunMapper.selectByRunId(runId);
        if (run == null || run.getDeletedTime() != null) {
            throw new MSException("运行记录不存在或已删除");
        }

        // 软删除运行记录
        run.setDeletedTime(System.currentTimeMillis());
        run.setUpdateTime(System.currentTimeMillis());
        workflowRunMapper.updateById(run);
    }

    /**
     * 根据运行ID和运行步骤ID获取执行日志
     */
    public List<Map<String, Object>> getRunLogsByRunIdAndRunStepId(String runId, String runStepId) {
        List<WorkflowRunLog> logs = workflowRunLogMapper.selectByRunIdAndRunStepId(runId, runStepId);
        return logs.stream().map(log -> {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("logId", log.getLogId());
            logMap.put("runId", log.getRunId());
            logMap.put("runStepId", log.getRunStepId());
            logMap.put("stepId", log.getStepId());
            logMap.put("level", log.getLevel());
            logMap.put("content", log.getContent());
            logMap.put("logTime", log.getLogTime());
            logMap.put("createTime", log.getCreateTime());
            return logMap;
        }).collect(Collectors.toList());
    }

    /**
     * 转换为 DTO
     */
    private WorkflowRunDTO convertToDTO(WorkflowRun run) {
        WorkflowRunDTO dto = new WorkflowRunDTO();
        dto.setRunId(run.getRunId());
        dto.setWorkflowId(run.getWorkflowId());
        dto.setProjectId(run.getProjectId());
        dto.setWorkflowName(run.getWorkflowName());
        dto.setTriggerType(run.getTriggerType());
        dto.setTriggerUser(run.getTriggerUser());
        dto.setStatus(run.getStatus());
        dto.setStartTime(run.getStartTime());
        dto.setEndTime(run.getEndTime());
        dto.setDurationMs(run.getDurationMs());
        dto.setTotalSteps(run.getTotalSteps());
        dto.setPassedCount(run.getPassedCount());
        dto.setFailedCount(run.getFailedCount());
        dto.setSkippedCount(run.getSkippedCount());
        dto.setPendingCount(run.getPendingCount());
        dto.setResultSummary(run.getResultSummary());
        dto.setEnvironmentId(run.getEnvironmentId());
        dto.setEnvironmentName(run.getEnvironmentName());
        return dto;
    }

    private List<Map<String, Object>> buildWorkflowStepsData(WorkflowRunDTO runDetail) {
        List<Map<String, Object>> stepsData = new ArrayList<>();
        if (runDetail.getSteps() == null) {
            return stepsData;
        }
        for (WorkflowRunStepDTO step : runDetail.getSteps()) {
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("runStepId", step.getRunStepId());
            stepMap.put("stepId", step.getStepId());
            stepMap.put("stepName", step.getStepName());
            stepMap.put("stepType", step.getStepType());
            stepMap.put("status", step.getStatus());
            stepMap.put("startTime", step.getStartTime());
            stepMap.put("endTime", step.getEndTime());
            stepMap.put("durationMs", step.getDurationMs());
            stepMap.put("requestData", step.getRequestData());
            stepMap.put("responseData", step.getResponseData());
            stepMap.put("assertion", step.getAssertion());
            stepMap.put("extractVars", step.getExtractVars());
            stepMap.put("errorMsg", step.getErrorMsg());
            stepMap.put("description", step.getDescription());
            stepsData.add(stepMap);
        }
        return stepsData;
    }

    /**
     * 转换步骤为 DTO
     */
    private WorkflowRunStepDTO convertStepToDTO(WorkflowRunStep step) {
        WorkflowRunStepDTO dto = new WorkflowRunStepDTO();
        dto.setRunStepId(step.getRunStepId());
        dto.setStepId(step.getStepId());
        dto.setStepName(step.getStepName());
        dto.setStepType(step.getStepType());
        dto.setStatus(step.getStatus());
        dto.setStartTime(step.getStartTime());
        dto.setEndTime(step.getEndTime());
        dto.setDurationMs(step.getDurationMs());
        dto.setRequestData(step.getRequestData());
        dto.setResponseData(step.getResponseData());
        dto.setAssertion(step.getAssertion());
        dto.setExtractVars(step.getExtractVars());
        dto.setErrorMsg(step.getErrorMsg());
        dto.setDescription(step.getDescription());
        return dto;
    }

    /**
     * 转换节点类型为执行机期望的格式
     * 执行机期望：http_request, mysql, dubbo, rocketmq, condition, loop 等小写加下划线格式
     */
    private String convertNodeTypeForExecutor(String nodeType) {
        if (nodeType == null) {
            return "http_request";
        }
        
        // 转换为小写
        String lowerType = nodeType.toLowerCase();
        
        // 处理常见的节点类型映射
        switch (lowerType) {
            case "http":
            case "http_request":
            case "api":
                return "http_request";
            case "sql":
            case "mysql":
                return "mysql";
            case "dubbo":
                return "dubbo";
            case "mq":
            case "rocketmq":
                return "rocketmq";
            case "condition":
            case "if":
                return "condition";
            case "loop":
            case "foreach":
                return "loop";
            case "script":
            case "python":
                return "script";
            case "xxl_job":
            case "xxljob":
                return "xxl_job";
            case "log_message":
            case "log":
                return "log_message";
            default:
                // 如果已经是下划线格式，直接返回；否则转换为下划线格式
                if (lowerType.contains("_")) {
                    return lowerType;
                } else {
                    // 简单转换：HTTP_REQUEST -> http_request
                    return lowerType.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
                }
        }
    }

    /**
     * 将 Object 转换为 Map（处理数组或对象的情况）
     * 如果输入是数组，返回空 Map；如果是 Map，直接返回；如果是 null，返回 null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        if (data instanceof List) {
            // 如果是数组，返回空 Map（或者可以根据需要转换为其他格式）
            return new HashMap<>();
        }
        // 其他类型，尝试转换为 Map
        return new HashMap<>();
    }

    /**
     * 处理执行机回调（调试运行 debugNode 与运行测试 execute 都会触发此接口）
     * 根据是否分批走 handleSingleCallback 或 handleBatchCallback，两处均保证先清空该 run 的旧步骤/日志再写入本次结果
     */
    public Map<String, Object> handleCallback(WorkflowRunResultCallbackRequest request) {
        String runId = request.getRunId();
        Map<String, Object> result = new HashMap<>();
        try {
            Integer batchIndex = request.getBatchIndex();
            Integer totalBatches = request.getTotalBatches();
            boolean isBatchMode = batchIndex != null && totalBatches != null && totalBatches > 1;
            
            if (isBatchMode) {
                return handleBatchCallback(request);
            } else {
                return handleSingleCallback(request);
            }
        } catch (Exception e) {
            log.error("处理执行机回调失败: runId={}, error={}", runId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "处理回调失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 处理分批回调（运行测试 execute 时执行机可能按批上报）
     * 第一批到达时先删除该 run 下所有旧步骤/日志，再按批写入，确保 assertion_logs/extract_vars 等为本次执行结果
     */
    private Map<String, Object> handleBatchCallback(WorkflowRunResultCallbackRequest request) {
        String runId = request.getRunId();
        Integer batchIndex = request.getBatchIndex();
        Integer totalBatches = request.getTotalBatches();
        boolean isFirstBatch = request.getIsFirstBatch() != null ? request.getIsFirstBatch() : (batchIndex == 1);
        boolean isLastBatch = request.getIsLastBatch() != null ? request.getIsLastBatch() : (batchIndex.equals(totalBatches));
        
        String redisKey = "workflow:callback:batch:" + runId;
        
        try {
            if (isFirstBatch) {
                // 第一批必须清空该 run 的旧步骤和日志，再写入本批，避免残留上一次执行的断言/提取
                workflowRunStepMapper.deleteByRunId(runId);
                workflowRunLogMapper.deleteByRunId(runId);
                
                // 保存基础信息到Redis（用于最后一批更新workflow_run表）
                Map<String, Object> baseInfo = new HashMap<>();
                baseInfo.put("runId", request.getRunId());
                baseInfo.put("status", request.getStatus());
                baseInfo.put("startTime", request.getStartTime());
                baseInfo.put("endTime", request.getEndTime());
                baseInfo.put("durationMs", request.getDurationMs());
                baseInfo.put("totalSteps", request.getTotalSteps());
                baseInfo.put("passedCount", request.getPassedCount());
                baseInfo.put("failedCount", request.getFailedCount());
                baseInfo.put("skippedCount", request.getSkippedCount());
                baseInfo.put("pendingCount", request.getPendingCount());
                baseInfo.put("resultSummary", request.getResultSummary());
                baseInfo.put("environmentName", request.getEnvironmentName());
                
                stringRedisTemplate.opsForValue().set(redisKey + ":base", 
                    objectMapper.writeValueAsString(baseInfo),
                    java.time.Duration.ofSeconds(BATCH_CALLBACK_EXPIRE_SECONDS));
            }
            
            saveBatchStepsAndLogs(request, runId);
            
            // 3. 记录已接收的批次索引
            stringRedisTemplate.opsForSet().add(redisKey + ":received", batchIndex.toString());
            stringRedisTemplate.expire(redisKey + ":received", 
                java.time.Duration.ofSeconds(BATCH_CALLBACK_EXPIRE_SECONDS));
            
            if (isLastBatch) {
                String baseInfoJson = stringRedisTemplate.opsForValue().get(redisKey + ":base");
                if (baseInfoJson != null) {
                    Map<String, Object> baseInfo = objectMapper.readValue(baseInfoJson, Map.class);
                    updateWorkflowRunSummary(runId, baseInfo);
                }
                try {
                    stringRedisTemplate.delete(redisKey + ":base");
                    stringRedisTemplate.delete(redisKey + ":received");
                } catch (Exception e) {
                    log.warn("清理批次数据失败: runId={}, error={}", runId, e.getMessage());
                }
            }
            
            Long receivedCount = stringRedisTemplate.opsForSet().size(redisKey + ":received");
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            if (isLastBatch) {
                result.put("message", String.format("所有批次已处理完成: %d/%d", receivedCount, totalBatches));
            } else {
                result.put("message", String.format("批次 %d/%d 已处理，等待其他批次...", batchIndex, totalBatches));
            }
            result.put("receivedBatches", receivedCount);
            result.put("totalBatches", totalBatches);
            return result;
            
        } catch (Exception e) {
            log.error("处理分批回调失败: runId={}, batchIndex={}, error={}", runId, batchIndex, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "处理分批回调失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 保存当前批次的 steps 和 logs 到数据库
     */
    private void saveBatchStepsAndLogs(WorkflowRunResultCallbackRequest request, String runId) {
        // 保存步骤执行明细
        if (CollectionUtils.isNotEmpty(request.getSteps())) {
            List<WorkflowRunStep> steps = new ArrayList<>();
            for (WorkflowRunResultCallbackRequest.WorkflowRunStepResult stepResult : request.getSteps()) {
                WorkflowRunStep step = convertStepResultToEntity(stepResult, runId);
                steps.add(step);
            }
            if (CollectionUtils.isNotEmpty(steps)) {
                workflowRunStepMapper.batchInsert(steps);
                log.info("已保存批次步骤数据: runId={}, steps={}", runId, steps.size());
            }
        }
        
        // 保存运行日志
        if (CollectionUtils.isNotEmpty(request.getLogs())) {
            final int MAX_LOG_CONTENT_LENGTH = 60000;
            List<WorkflowRunLog> logs = new ArrayList<>();
            for (WorkflowRunResultCallbackRequest.WorkflowRunLogResult logResult : request.getLogs()) {
                WorkflowRunLog runLog = new WorkflowRunLog();
                runLog.setRunId(runId);
                runLog.setRunStepId(logResult.getRunStepId());
                runLog.setStepId(logResult.getStepId());
                runLog.setLevel(logResult.getLevel());
                
                // 截断过长的日志内容
                String content = logResult.getContent();
                if (content != null && content.length() > MAX_LOG_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_LOG_CONTENT_LENGTH) + 
                        "\n... [日志内容过长，已截断，原长度: " + logResult.getContent().length() + " 字符]";
                    log.warn("日志内容过长，已截断: runId={}, stepId={}, 原长度={}", 
                        runId, logResult.getStepId(), logResult.getContent().length());
                }
                runLog.setContent(content);
                
                runLog.setLogTime(logResult.getLogTime() != null ? logResult.getLogTime() : System.currentTimeMillis());
                runLog.setCreateTime(System.currentTimeMillis());
                logs.add(runLog);
            }
            if (CollectionUtils.isNotEmpty(logs)) {
                workflowRunLogMapper.batchInsert(logs);
                log.info("已保存批次日志数据: runId={}, logs={}", runId, logs.size());
            }
        }
    }
    
    /**
     * 将 StepResult 转换为 WorkflowRunStep 实体
     */
    private WorkflowRunStep convertStepResultToEntity(
            WorkflowRunResultCallbackRequest.WorkflowRunStepResult stepResult, String runId) {
        WorkflowRunStep step = new WorkflowRunStep();
        step.setRunStepId(IDGenerator.nextStr());
        step.setRunId(runId);
        step.setStepId(stepResult.getStepId());
        step.setStepName(stepResult.getStepName());
        step.setStepType(stepResult.getStepType());
        step.setOrderNum(stepResult.getOrderNum());
        step.setStatus(stepResult.getStatus());
        step.setStartTime(stepResult.getStartTime());
        step.setEndTime(stepResult.getEndTime());
        step.setDurationMs(stepResult.getDurationMs());
        
        // 处理 requestData
        step.setRequestData(convertToMap(stepResult.getRequestData()));
        
        // 处理 responseData
        Map<String, Object> responseDataMap = convertToMap(stepResult.getResponseData());
        
        // 从 responseData 中提取 assertion 和 extractVars
        List<Map<String, Object>> assertion = stepResult.getAssertion();
        if ((assertion == null || assertion.isEmpty()) && responseDataMap != null) {
            Object assertionObj = responseDataMap.get("assertion");
            if (assertionObj instanceof List) {
                List<?> assertionList = (List<?>) assertionObj;
                if (!assertionList.isEmpty()) {
                    assertion = new ArrayList<>();
                    for (Object item : assertionList) {
                        if (item instanceof Map) {
                            assertion.add((Map<String, Object>) item);
                        }
                    }
                }
            } else if (assertionObj instanceof Map) {
                assertion = new ArrayList<>();
                assertion.add((Map<String, Object>) assertionObj);
            }
        }
        
        Object extractVars = stepResult.getExtractVars();
        if (extractVars == null && responseDataMap != null) {
            extractVars = responseDataMap.get("extractVars");
        }
        
        // 从 responseData 中移除 assertion 和 extractVars
        if (responseDataMap != null) {
            responseDataMap.remove("assertion");
            responseDataMap.remove("extractVars");
        }
        
        step.setResponseData(responseDataMap);
        step.setAssertion(assertion != null ? assertion : new ArrayList<>());
        step.setExtractVars(convertToMap(extractVars));
        step.setErrorMsg(stepResult.getErrorMsg());
        step.setErrorStack(stepResult.getErrorStack());
        step.setDescription(stepResult.getDescription());
        step.setCreateTime(System.currentTimeMillis());
        step.setUpdateTime(System.currentTimeMillis());
        return step;
    }
    
    /**
     * 更新 workflow_run 表的汇总信息
     */
    private void updateWorkflowRunSummary(String runId, Map<String, Object> baseInfo) {
        WorkflowRun run = workflowRunMapper.selectByRunId(runId);
        if (run == null) {
            log.warn("运行记录不存在，无法更新汇总信息: runId={}", runId);
            return;
        }
        
        run.setStatus((String) baseInfo.get("status"));
        
        Object startTimeObj = baseInfo.get("startTime");
        run.setStartTime(startTimeObj != null ? 
            (startTimeObj instanceof Long ? (Long) startTimeObj : Long.valueOf(startTimeObj.toString())) : null);
        
        Object endTimeObj = baseInfo.get("endTime");
        run.setEndTime(endTimeObj != null ? 
            (endTimeObj instanceof Long ? (Long) endTimeObj : Long.valueOf(endTimeObj.toString())) : null);
        
        Object durationMsObj = baseInfo.get("durationMs");
        run.setDurationMs(durationMsObj != null ? 
            (durationMsObj instanceof Long ? (Long) durationMsObj : Long.valueOf(durationMsObj.toString())) : null);
        
        Object totalStepsObj = baseInfo.get("totalSteps");
        run.setTotalSteps(totalStepsObj != null ? 
            (totalStepsObj instanceof Integer ? (Integer) totalStepsObj : Integer.valueOf(totalStepsObj.toString())) : null);
        
        Object passedCountObj = baseInfo.get("passedCount");
        run.setPassedCount(passedCountObj != null ? 
            (passedCountObj instanceof Integer ? (Integer) passedCountObj : Integer.valueOf(passedCountObj.toString())) : null);
        
        Object failedCountObj = baseInfo.get("failedCount");
        run.setFailedCount(failedCountObj != null ? 
            (failedCountObj instanceof Integer ? (Integer) failedCountObj : Integer.valueOf(failedCountObj.toString())) : null);
        
        Object skippedCountObj = baseInfo.get("skippedCount");
        run.setSkippedCount(skippedCountObj != null ? 
            (skippedCountObj instanceof Integer ? (Integer) skippedCountObj : Integer.valueOf(skippedCountObj.toString())) : null);
        
        Object pendingCountObj = baseInfo.get("pendingCount");
        run.setPendingCount(pendingCountObj != null ? 
            (pendingCountObj instanceof Integer ? (Integer) pendingCountObj : Integer.valueOf(pendingCountObj.toString())) : null);
        
        run.setResultSummary((Map<String, Object>) baseInfo.get("resultSummary"));
        run.setUpdateTime(System.currentTimeMillis());
        workflowRunMapper.updateById(run);
        
        log.info("已更新运行记录汇总信息: runId={}", runId);
        
        // 更新测试报告（如果关联了报告）
        if (StringUtils.isNotBlank(run.getReportId())) {
            try {
                workflowTestReportService.updateReportOnWorkflowComplete(run.getReportId());
                log.info("已更新测试报告: reportId={}", run.getReportId());
            } catch (Exception e) {
                log.error("更新测试报告失败: reportId={}, error={}", run.getReportId(), e.getMessage(), e);
            }
        }
        
        // 通过 WebSocket 推送执行结果给前端
        try {
            WorkflowRunDTO runDetail = getRunDetail(runId);
            
            // 转换步骤数据为 Map 格式
            List<Map<String, Object>> stepsData = new ArrayList<>();
            if (runDetail.getSteps() != null) {
                for (WorkflowRunStepDTO step : runDetail.getSteps()) {
                    Map<String, Object> stepMap = new HashMap<>();
                    stepMap.put("runStepId", step.getRunStepId());
                    stepMap.put("stepId", step.getStepId());
                    stepMap.put("stepName", step.getStepName());
                    stepMap.put("stepType", step.getStepType());
                    stepMap.put("status", step.getStatus());
                    stepMap.put("startTime", step.getStartTime());
                    stepMap.put("endTime", step.getEndTime());
                    stepMap.put("durationMs", step.getDurationMs());
                    stepMap.put("requestData", step.getRequestData());
                    stepMap.put("responseData", step.getResponseData());
                    stepMap.put("assertion", step.getAssertion());
                    stepMap.put("extractVars", step.getExtractVars());
                    stepMap.put("errorMsg", step.getErrorMsg());
                    stepMap.put("description", step.getDescription());
                    stepsData.add(stepMap);
                }
            }
            
            // 构建描述信息
            String description = "工作流执行完成";
            if ("SUCCESS".equals(runDetail.getStatus()) || "SUCCEED".equals(runDetail.getStatus())) {
                description = "工作流执行成功";
            } else if ("FAILED".equals(runDetail.getStatus()) || "FAIL".equals(runDetail.getStatus())) {
                description = "工作流执行失败";
            }
            
            io.vanguard.testops.workflow.support.socket.WorkflowWebSocketHandler.sendWorkflowResult(
                runId,
                runDetail.getStatus(),
                description,
                stepsData,
                null
            );
        } catch (Exception e) {
            log.error("推送执行结果失败: runId={}, error={}", runId, e.getMessage(), e);
            // WebSocket 推送失败不影响主流程，只记录日志
        }
    }
    
    
    /**
     * 处理单次回调（调试运行 debugNode 或运行测试 execute 单次上报时走此分支）
     * 每次先删除该 run 下旧步骤/日志，再写入本次回调数据，确保 assertion_logs/extract_vars 为本次执行结果
     */
    private Map<String, Object> handleSingleCallback(WorkflowRunResultCallbackRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            applyWorkflowRunResult(request);
            result.put("success", true);
            result.put("message", "回调处理成功");
            return result;
        } catch (Exception e) {
            log.error("处理执行机回调失败: runId={}, error={}", request.getRunId(), e.getMessage(), e);
            result.put("success", false);
            result.put("message", "处理回调失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 应用单条工作流执行结果：更新 run、写 steps/logs、更新报告、推送 WebSocket。
     * 供 HTTP 回调与 Kafka 消费者共用，保证运行/调试前端状态更新一致。
     */
    public void applyWorkflowRunResult(WorkflowRunResultCallbackRequest request) {
        WorkflowRun run = workflowRunMapper.selectByRunId(request.getRunId());
        if (run == null) {
            log.warn("运行记录不存在: runId={}", request.getRunId());
            return;
        }

            // 2. 更新运行记录
            run.setStatus(request.getStatus());
            if (request.getStartTime() != null) {
                run.setStartTime(request.getStartTime());
            }
            if (request.getEndTime() != null) {
                run.setEndTime(request.getEndTime());
            }
            if (request.getDurationMs() != null) {
                run.setDurationMs(request.getDurationMs());
            }
            if (request.getTotalSteps() != null) {
                run.setTotalSteps(request.getTotalSteps());
            }
            if (request.getPassedCount() != null) {
                run.setPassedCount(request.getPassedCount());
            }
            if (request.getFailedCount() != null) {
                run.setFailedCount(request.getFailedCount());
            }
            if (request.getSkippedCount() != null) {
                run.setSkippedCount(request.getSkippedCount());
            }
            if (request.getPendingCount() != null) {
                run.setPendingCount(request.getPendingCount());
            }
            if (request.getResultSummary() != null) {
                run.setResultSummary(request.getResultSummary());
            }
            // 注意：environment_name 和 module_id 在创建运行记录时已设置，执行机回调时不会提供，所以不更新
            run.setUpdateTime(System.currentTimeMillis());
            workflowRunMapper.updateById(run);

            // 每次回调都先删除该 run 下的旧步骤，再写入本次回调的步骤，确保 assertion_logs/extract_vars 等始终为本次执行结果（避免展示上一次的断言/提取）
            workflowRunStepMapper.deleteByRunId(request.getRunId());
            if (CollectionUtils.isNotEmpty(request.getSteps())) {
                // 批量插入新的步骤记录
                List<WorkflowRunStep> steps = new ArrayList<>();
                for (WorkflowRunResultCallbackRequest.WorkflowRunStepResult stepResult : request.getSteps()) {
                    WorkflowRunStep step = new WorkflowRunStep();
                    step.setRunStepId(IDGenerator.nextStr());
                    step.setRunId(request.getRunId());
                    step.setStepId(stepResult.getStepId());
                    step.setStepName(stepResult.getStepName());
                    step.setStepType(stepResult.getStepType());
                    step.setOrderNum(stepResult.getOrderNum());
                    step.setStatus(stepResult.getStatus());
                    step.setStartTime(stepResult.getStartTime());
                    step.setEndTime(stepResult.getEndTime());
                    step.setDurationMs(stepResult.getDurationMs());
                    // 处理 requestData：如果是数组，转换为 Map；如果是 Map，直接使用
                    step.setRequestData(convertToMap(stepResult.getRequestData()));
                    
                    // 处理 responseData：如果是数组，转换为 Map；如果是 Map，直接使用
                    Map<String, Object> responseDataMap = convertToMap(stepResult.getResponseData());
                    
                    // 从 responseData 中提取 assertion 和 extractVars（如果独立字段为 null 或空）
                    List<Map<String, Object>> assertion = stepResult.getAssertion();
                    // 如果独立字段为 null 或空列表，尝试从 responseData 中提取
                    if ((assertion == null || assertion.isEmpty()) && responseDataMap != null) {
                        Object assertionObj = responseDataMap.get("assertion");
                        if (assertionObj instanceof List) {
                            List<?> assertionList = (List<?>) assertionObj;
                            if (!assertionList.isEmpty()) {
                                assertion = new ArrayList<>();
                                for (Object item : assertionList) {
                                    if (item instanceof Map) {
                                        assertion.add((Map<String, Object>) item);
                                    }
                                }
                            }
                        } else if (assertionObj instanceof Map) {
                            assertion = new ArrayList<>();
                            assertion.add((Map<String, Object>) assertionObj);
                        }
                    }
                    
                    Object extractVars = stepResult.getExtractVars();
                    // 如果独立字段为 null，尝试从 responseData 中提取
                    if (extractVars == null && responseDataMap != null) {
                        extractVars = responseDataMap.get("extractVars");
                    }
                    
                    // 从 responseData 中移除 assertion 和 extractVars，避免重复存储
                    if (responseDataMap != null) {
                        responseDataMap.remove("assertion");
                        responseDataMap.remove("extractVars");
                    }
                    
                    step.setResponseData(responseDataMap);
                    step.setAssertion(assertion != null ? assertion : new ArrayList<>());
                    // 处理 extractVars：如果是数组，转换为 Map；如果是 Map，直接使用
                    step.setExtractVars(convertToMap(extractVars));
                    step.setErrorMsg(stepResult.getErrorMsg());
                    step.setErrorStack(stepResult.getErrorStack());
                    step.setDescription(stepResult.getDescription());
                    step.setCreateTime(System.currentTimeMillis());
                    step.setUpdateTime(System.currentTimeMillis());
                    steps.add(step);
                }
                if (CollectionUtils.isNotEmpty(steps)) {
                    workflowRunStepMapper.batchInsert(steps);
                }
            }

            // 4. 保存运行日志（先删旧再插新，与步骤一致）
            workflowRunLogMapper.deleteByRunId(request.getRunId());
            if (CollectionUtils.isNotEmpty(request.getLogs())) {
                // 批量插入新的日志记录
                // 注意：日志内容可能很长，需要截断以防止数据库存储失败
                // MySQL TEXT 类型最大 65535 字节，LONGTEXT 最大 4GB
                // 这里使用 60000 字符作为安全限制（考虑多字节字符）
                final int MAX_LOG_CONTENT_LENGTH = 60000;
                
                List<WorkflowRunLog> logs = new ArrayList<>();
                for (WorkflowRunResultCallbackRequest.WorkflowRunLogResult logResult : request.getLogs()) {
                    WorkflowRunLog runLog = new WorkflowRunLog();
                    runLog.setRunId(request.getRunId());
                    runLog.setRunStepId(logResult.getRunStepId());
                    runLog.setStepId(logResult.getStepId());
                    runLog.setLevel(logResult.getLevel());
                    
                    // 截断过长的日志内容，防止数据库存储失败
                    String content = logResult.getContent();
                    if (content != null && content.length() > MAX_LOG_CONTENT_LENGTH) {
                        content = content.substring(0, MAX_LOG_CONTENT_LENGTH) + "\n... [日志内容过长，已截断，原长度: " + logResult.getContent().length() + " 字符]";
                        log.warn("日志内容过长，已截断: runId={}, stepId={}, 原长度={}", 
                            request.getRunId(), logResult.getStepId(), logResult.getContent().length());
                    }
                    runLog.setContent(content);
                    
                    runLog.setLogTime(logResult.getLogTime() != null ? logResult.getLogTime() : System.currentTimeMillis());
                    runLog.setCreateTime(System.currentTimeMillis());
                    logs.add(runLog);
                }
                if (CollectionUtils.isNotEmpty(logs)) {
                    workflowRunLogMapper.batchInsert(logs);
                }
            }

            log.info("执行机回调处理成功: runId={}, status={}", request.getRunId(), request.getStatus());

            // 5. 更新测试报告（如果关联了报告）
            if (StringUtils.isNotBlank(run.getReportId())) {
                try {
                    workflowTestReportService.updateReportOnWorkflowComplete(run.getReportId());
                    log.info("已更新测试报告: reportId={}", run.getReportId());
                } catch (Exception e) {
                    log.error("更新测试报告失败: reportId={}, error={}", run.getReportId(), e.getMessage(), e);
                    // 更新失败不影响主流程，只记录日志
                }
            }

            // 6. 通过 WebSocket 推送执行结果给前端
            try {
                // 获取完整的工作流执行详情（包含所有步骤数据）
                WorkflowRunDTO runDetail = getRunDetail(request.getRunId());
                
                // 转换步骤数据为 Map 格式
                List<Map<String, Object>> stepsData = new ArrayList<>();
                if (runDetail.getSteps() != null) {
                    for (WorkflowRunStepDTO step : runDetail.getSteps()) {
                        Map<String, Object> stepMap = new HashMap<>();
                        stepMap.put("runStepId", step.getRunStepId());
                        stepMap.put("stepId", step.getStepId());
                        stepMap.put("stepName", step.getStepName());
                        stepMap.put("stepType", step.getStepType());
                        stepMap.put("status", step.getStatus());
                        stepMap.put("startTime", step.getStartTime());
                        stepMap.put("endTime", step.getEndTime());
                        stepMap.put("durationMs", step.getDurationMs());
                        stepMap.put("requestData", step.getRequestData());
                        stepMap.put("responseData", step.getResponseData());
                        stepMap.put("assertion", step.getAssertion());
                        stepMap.put("extractVars", step.getExtractVars());
                        stepMap.put("errorMsg", step.getErrorMsg());
                        stepMap.put("errorStack", null); // WorkflowRunStepDTO 中没有 errorStack 字段
                        stepMap.put("description", step.getDescription());
                        stepsData.add(stepMap);
                    }
                }
                
                // 构建描述信息
                String description = "工作流执行完成";
                if ("SUCCESS".equals(runDetail.getStatus()) || "SUCCEED".equals(runDetail.getStatus())) {
                    description = "工作流执行成功";
                } else if ("FAILED".equals(runDetail.getStatus()) || "FAIL".equals(runDetail.getStatus())) {
                    description = "工作流执行失败";
                }
                
                io.vanguard.testops.workflow.support.socket.WorkflowWebSocketHandler.sendWorkflowResult(
                    request.getRunId(),
                    runDetail.getStatus(),
                    description,
                    stepsData,
                    null
                );
            } catch (Exception e) {
                log.error("推送 WebSocket 消息失败: runId={}, error={}", request.getRunId(), e.getMessage(), e);
            }
    }

    /**
     * 将 triggerType 映射为 reportType
     * MANUAL -> MANUAL (手动生成)
     * SCHEDULE -> SCHEDULE (定时生成)
     * API -> AUTO (自动生成)
     */
    private String mapTriggerTypeToReportType(String triggerType) {
        if (StringUtils.isBlank(triggerType)) {
            return "MANUAL";
        }
        switch (triggerType.toUpperCase()) {
            case "MANUAL":
                return "MANUAL";
            case "SCHEDULE":
                return "SCHEDULE";
            case "API":
                return "AUTO";
            default:
                return "MANUAL"; // 默认值
        }
    }

    /**
     * 组装单个 workflow 的数据（用于批量执行）
     * @param workflowId 工作流ID
     * @param definition 工作流定义
     * @param profile 环境配置
     * @param runId 运行ID
     * @param userVariables 用户输入的变量（优先级最高）
     * @return workflowItem Map，包含 workflow、variables、runId
     */
    /**
     * 将「引用子工作流」节点按引用顺序内联展开（支持子调子递归展开）：用子工作流的节点/连线替换该节点，统一使用父环境。
     * 循环引用时跳过该节点并做直通连线；变量传递保持现状不单独处理。
     *
     * @param workflowId 根工作流 ID（所有展开后的 step 的 workflowId 均为此）
     * @param stepsIn   当前工作流步骤
     * @param linksIn   当前工作流连线
     * @param stepsOut  输出：展开后的步骤列表（不含 SUB_WORKFLOW 节点本身，已替换为子工作流节点）
     * @param linksOut  输出：展开后的连线列表（含子工作流内连线和与原边界的衔接连线）
     */
    private void expandSubWorkflows(String workflowId, List<WorkflowStep> stepsIn, List<WorkflowStepLink> linksIn,
                                    List<WorkflowStep> stepsOut, List<WorkflowStepLink> linksOut) {
        expandSubWorkflowsRecursive(stepsIn, linksIn, stepsOut, linksOut, workflowId, "", new HashSet<>());
    }

    /**
     * 递归展开子工作流。子工作流内若仍包含 SUB_WORKFLOW 节点会继续展开；若 refWorkflowId 已在 expandedWorkflowIds 中则视为循环引用并跳过。
     *
     * @param stepsIn              当前层步骤
     * @param linksIn              当前层连线
     * @param stepsOut             输出步骤（带 currentPrefix 的 stepId）
     * @param linksOut             输出连线
     * @param parentWorkflowId     根工作流 ID（所有 step 的 workflowId）
     * @param currentPrefix        当前层前缀，顶层为空串；递归时为 父层 prefix + 当前 SUB_WORKFLOW 的 stepId + "_"
     * @param expandedWorkflowIds  已进入展开链的 workflowId 集合，用于检测 A→B→A 循环
     */
    private void expandSubWorkflowsRecursive(List<WorkflowStep> stepsIn, List<WorkflowStepLink> linksIn,
                                             List<WorkflowStep> stepsOut, List<WorkflowStepLink> linksOut,
                                             String parentWorkflowId, String currentPrefix, Set<String> expandedWorkflowIds) {
        if (stepsIn == null) stepsIn = new ArrayList<>();
        if (linksIn == null) linksIn = new ArrayList<>();

        // 当前层「引用子节点」stepId（未带 prefix）-> 展开后的入口/出口 stepId（已带完整 prefix，即 stepsOut 中的 id）
        Map<String, SubExpandInfo> subExpandMap = new HashMap<>();
        // 被跳过的「引用子节点」stepId（未带 prefix）-> 用于连线的 workflowId
        Map<String, String> skippedSubWorkflowStepIds = new HashMap<>();

        for (WorkflowStep step : stepsIn) {
            if (!"SUB_WORKFLOW".equalsIgnoreCase(step.getStepType())) {
                WorkflowStep copy = copyStepWithPrefix(step, currentPrefix, parentWorkflowId);
                stepsOut.add(copy);
                continue;
            }
            if (StringUtils.isBlank(step.getRefWorkflowId())) {
                skippedSubWorkflowStepIds.put(step.getStepId(), step.getWorkflowId());
                continue;
            }

            String refWorkflowId = step.getRefWorkflowId();
            if (expandedWorkflowIds.contains(refWorkflowId)) {
                skippedSubWorkflowStepIds.put(step.getStepId(), step.getWorkflowId());
                continue;
            }

            List<WorkflowStep> subSteps = workflowStepMapper.selectByWorkflowId(refWorkflowId);
            List<WorkflowStepLink> subLinks = workflowStepLinkMapper.selectByWorkflowId(refWorkflowId);
            if (subLinks == null) subLinks = new ArrayList<>();
            if (CollectionUtils.isEmpty(subSteps)) {
                skippedSubWorkflowStepIds.put(step.getStepId(), step.getWorkflowId());
                continue;
            }

            String subPrefix = currentPrefix + step.getStepId() + "_";
            Set<String> childExpanded = new HashSet<>(expandedWorkflowIds);
            childExpanded.add(refWorkflowId);

            List<WorkflowStep> subStepsOut = new ArrayList<>();
            List<WorkflowStepLink> subLinksOut = new ArrayList<>();
            expandSubWorkflowsRecursive(subSteps, subLinks, subStepsOut, subLinksOut, parentWorkflowId, subPrefix, childExpanded);

            Set<String> subLinkTargets = subLinksOut.stream().map(WorkflowStepLink::getTargetStepId).collect(Collectors.toSet());
            Set<String> subLinkSources = subLinksOut.stream().map(WorkflowStepLink::getSourceStepId).collect(Collectors.toSet());
            List<String> entryIdsFull = subStepsOut.stream()
                    .map(WorkflowStep::getStepId)
                    .filter(id -> !subLinkTargets.contains(id))
                    .collect(Collectors.toList());
            List<String> exitIdsFull = subStepsOut.stream()
                    .map(WorkflowStep::getStepId)
                    .filter(id -> !subLinkSources.contains(id))
                    .collect(Collectors.toList());
            if (entryIdsFull.isEmpty()) entryIdsFull = java.util.Collections.singletonList(subStepsOut.get(0).getStepId());
            if (exitIdsFull.isEmpty()) exitIdsFull = java.util.Collections.singletonList(subStepsOut.get(subStepsOut.size() - 1).getStepId());

            subExpandMap.put(step.getStepId(), new SubExpandInfo(entryIdsFull, exitIdsFull));
            stepsOut.addAll(subStepsOut);
            linksOut.addAll(subLinksOut);
        }

        Map<String, List<String>> skippedIncoming = new HashMap<>();
        Map<String, List<String>> skippedOutgoing = new HashMap<>();
        for (String sid : skippedSubWorkflowStepIds.keySet()) {
            skippedIncoming.put(sid, new ArrayList<>());
            skippedOutgoing.put(sid, new ArrayList<>());
        }
        for (WorkflowStepLink link : linksIn) {
            String targetId = link.getTargetStepId();
            String sourceId = link.getSourceStepId();
            String sourceIdOut = currentPrefix + sourceId;
            String targetIdOut = currentPrefix + targetId;
            if (skippedSubWorkflowStepIds.containsKey(targetId)) {
                skippedIncoming.get(targetId).add(sourceIdOut);
                continue;
            }
            if (skippedSubWorkflowStepIds.containsKey(sourceId)) {
                skippedOutgoing.get(sourceId).add(targetIdOut);
                continue;
            }
            SubExpandInfo inInfo = subExpandMap.get(targetId);
            SubExpandInfo outInfo = subExpandMap.get(sourceId);
            if (inInfo != null && outInfo != null) {
                for (String exitId : outInfo.exitIds) {
                    for (String entryId : inInfo.entryIds) {
                        linksOut.add(copyLinkWithEndpoints(null, link, exitId, entryId, parentWorkflowId));
                    }
                }
            } else if (inInfo != null) {
                for (String entryId : inInfo.entryIds) {
                    linksOut.add(copyLinkWithEndpoints(null, link, sourceIdOut, entryId, parentWorkflowId));
                }
            } else if (outInfo != null) {
                for (String exitId : outInfo.exitIds) {
                    linksOut.add(copyLinkWithEndpoints(null, link, exitId, targetIdOut, parentWorkflowId));
                }
            } else {
                WorkflowStepLink linkCopy = new WorkflowStepLink();
                linkCopy.setLinkId(currentPrefix + link.getLinkId());
                linkCopy.setWorkflowId(parentWorkflowId);
                linkCopy.setSourceStepId(sourceIdOut);
                linkCopy.setTargetStepId(targetIdOut);
                linkCopy.setLabel(link.getLabel());
                linkCopy.setColor(link.getColor());
                linkCopy.setConditionExpr(link.getConditionExpr());
                linkCopy.setOrderNum(link.getOrderNum());
                linkCopy.setCreateTime(link.getCreateTime());
                linkCopy.setUpdateTime(link.getUpdateTime());
                linksOut.add(linkCopy);
            }
        }
        for (Map.Entry<String, String> e : skippedSubWorkflowStepIds.entrySet()) {
            String skippedId = e.getKey();
            String workflowIdForLink = e.getValue();
            List<String> sources = skippedIncoming.get(skippedId);
            List<String> targets = skippedOutgoing.get(skippedId);
            if (sources != null && targets != null && !sources.isEmpty() && !targets.isEmpty()) {
                for (int i = 0; i < sources.size(); i++) {
                    for (int j = 0; j < targets.size(); j++) {
                        WorkflowStepLink passThrough = new WorkflowStepLink();
                        passThrough.setLinkId(currentPrefix + skippedId + "_passthrough_" + i + "_" + j);
                        passThrough.setWorkflowId(workflowIdForLink);
                        passThrough.setSourceStepId(sources.get(i));
                        passThrough.setTargetStepId(targets.get(j));
                        passThrough.setCreateTime(System.currentTimeMillis());
                        passThrough.setUpdateTime(System.currentTimeMillis());
                        linksOut.add(passThrough);
                    }
                }
            }
        }
    }

    private WorkflowStep copyStepWithPrefix(WorkflowStep step, String prefix, String parentWorkflowId) {
        WorkflowStep copy = new WorkflowStep();
        copy.setStepId(prefix + step.getStepId());
        copy.setWorkflowId(parentWorkflowId);
        copy.setName(step.getName());
        copy.setStepType(step.getStepType());
        copy.setOrderNum(step.getOrderNum());
        copy.setStepConfig(step.getStepConfig() != null ? new HashMap<>(step.getStepConfig()) : new HashMap<>());
        copy.setPositionX(step.getPositionX());
        copy.setPositionY(step.getPositionY());
        copy.setRefMode(step.getRefMode());
        copy.setRefMetadataId(step.getRefMetadataId());
        copy.setRefWorkflowId(step.getRefWorkflowId());
        copy.setEnable(step.getEnable());
        copy.setCreateTime(step.getCreateTime());
        copy.setUpdateTime(step.getUpdateTime());
        return copy;
    }

    private WorkflowStepLink copyLinkWithEndpoints(String linkId, WorkflowStepLink link, String sourceStepId, String targetStepId, String workflowId) {
        WorkflowStepLink boundary = new WorkflowStepLink();
        boundary.setLinkId(linkId != null ? linkId : sourceStepId + "_to_" + targetStepId);
        boundary.setWorkflowId(workflowId);
        boundary.setSourceStepId(sourceStepId);
        boundary.setTargetStepId(targetStepId);
        boundary.setLabel(link.getLabel());
        boundary.setColor(link.getColor());
        boundary.setConditionExpr(link.getConditionExpr());
        boundary.setOrderNum(link.getOrderNum());
        boundary.setCreateTime(link.getCreateTime());
        boundary.setUpdateTime(link.getUpdateTime());
        return boundary;
    }

    private static class SubExpandInfo {
        final List<String> entryIds;
        final List<String> exitIds;

        SubExpandInfo(List<String> entryIds, List<String> exitIds) {
            this.entryIds = entryIds;
            this.exitIds = exitIds;
        }
    }

    private Map<String, Object> buildWorkflowItem(String workflowId, WorkflowDefinition definition, 
                                                   WorkflowEngineProfile profile, String runId, 
                                                   Map<String, String> userVariables) {
        // 获取节点和连线数据
        List<WorkflowStep> steps = workflowStepMapper.selectByWorkflowId(workflowId);
        List<WorkflowStepLink> links = workflowStepLinkMapper.selectByWorkflowId(workflowId);

        // 将引用子工作流按引用顺序内联展开（支持子调子递归），统一使用父环境
        List<WorkflowStep> expandedSteps = new ArrayList<>();
        List<WorkflowStepLink> expandedLinks = new ArrayList<>();
        expandSubWorkflows(workflowId, steps, links, expandedSteps, expandedLinks);
        steps = expandedSteps;
        links = expandedLinks;
        
        // 转换为执行机需要的节点格式：{id, type, data: {config, assertion, extractions}}
        List<Map<String, Object>> nodes = new ArrayList<>();
        // 收集所有节点的environmentVariables和节点类型，用于后续合并到variables中
        List<NodeEnvVarsWithType> allNodeEnvironmentVariables = new ArrayList<>();
        
        if (steps != null) {
            for (WorkflowStep step : steps) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", step.getStepId());
                node.put("stepName", step.getName()); 
                
                // 转换节点类型（执行机期望小写加下划线格式）
                String nodeType = convertNodeTypeForExecutor(step.getStepType());
                node.put("type", nodeType);
                
                // 节点数据：包含 config, assertion, extractions
                Map<String, Object> stepConfig = step.getStepConfig() != null ? step.getStepConfig() : new HashMap<>();
                Map<String, Object> nodeData = new HashMap<>();
                
                // config：从 stepConfig 中提取，如果不存在则使用整个 stepConfig
                Map<String, Object> config;
                if (stepConfig.containsKey("config")) {
                    config = new HashMap<>((Map<String, Object>) stepConfig.get("config"));
                } else {
                    // 如果没有 config 字段，将整个 stepConfig 作为 config（排除 assertion 和 extractions）
                    config = new HashMap<>(stepConfig);
                    config.remove("assertion");
                    config.remove("extractions");
                }
                
                // HTTP_REQUEST 节点特殊处理：根据 paramType 只保留对应字段（需要在processNodeConfig之前处理）
                if ("http_request".equals(nodeType)) {
                    String paramType = (String) config.get("paramType");
                    if (StringUtils.isNotBlank(paramType)) {
                        // 先备份一份原始配置，用于从中恢复各个字段
                        Map<String, Object> originalConfig;
                        if (stepConfig.containsKey("config")) {
                            originalConfig = (Map<String, Object>) stepConfig.get("config");
                        } else {
                            originalConfig = stepConfig;
                        }
                        
                        // 移除所有参数类型字段
                        config.remove("params");
                        config.remove("json");
                        config.remove("data");
                        config.remove("upload");
                        config.remove("body");
                        
                        if ("upload".equals(paramType)) {
                            // upload 模式：需要同时保留文件字段(upload)和表单字段(data)
                            if (originalConfig.containsKey("upload")) {
                                config.put("upload", originalConfig.get("upload"));
                            }
                            if (originalConfig.containsKey("data")) {
                                config.put("data", originalConfig.get("data"));
                            }
                        } else {
                            // 其他模式：保持旧逻辑，只恢复对应一种字段
                            if (originalConfig.containsKey(paramType)) {
                                config.put(paramType, originalConfig.get(paramType));
                            }
                        }
                    }
                }
                
                // 使用统一的节点配置处理方法
                NodeConfigProcessResult processResult = processNodeConfig(config, nodeType, userVariables, profile);
                config = processResult.config;
                Map<String, Object> nodeEnvironmentVariables = processResult.environmentVariables;
                
                // 收集节点的environmentVariables和节点类型，后续统一合并到variables中
                if (!nodeEnvironmentVariables.isEmpty()) {
                    NodeEnvVarsWithType envVarsWithType = new NodeEnvVarsWithType();
                    envVarsWithType.nodeType = nodeType;
                    envVarsWithType.environmentVariables = nodeEnvironmentVariables;
                    allNodeEnvironmentVariables.add(envVarsWithType);
                }
                
                nodeData.put("config", config);
                
                // assertion：从 stepConfig 中提取，如果不存在则为空对象
                if (stepConfig.containsKey("assertion")) {
                    nodeData.put("assertion", stepConfig.get("assertion"));
                } else {
                    nodeData.put("assertion", new HashMap<>());
                }
                
                // extractions：从 stepConfig 中提取，如果不存在则为空数组
                if (stepConfig.containsKey("extractions")) {
                    nodeData.put("extractions", stepConfig.get("extractions"));
                } else {
                    nodeData.put("extractions", new ArrayList<>());
                }
                
                node.put("data", nodeData);
                nodes.add(node);
            }
        }
        
        // 转换为执行机需要的边格式：{id, source, target, source_handle?}
        List<Map<String, Object>> edges = new ArrayList<>();
        if (links != null) {
            int edgeIndex = 1;
            for (WorkflowStepLink link : links) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("id", "e" + edgeIndex++);
                edge.put("source", link.getSourceStepId());
                edge.put("target", link.getTargetStepId());
                // 如果有 label（条件分支），设置 source_handle
                if (StringUtils.isNotBlank(link.getLabel())) {
                    edge.put("source_handle", link.getLabel());
                }
                edges.add(edge);
            }
        }

        // 工作流对象（包装在 workflow 字段中）
        Map<String, Object> workflowObj = new HashMap<>();
        workflowObj.put("workflowId", workflowId);
        workflowObj.put("workflowName", definition.getName());
        workflowObj.put("nodes", nodes);
        workflowObj.put("edges", edges);
        
        // 全局变量（执行机期望的格式，放在每个 workflow 对象内的 variables 字段）
        Map<String, Object> variables = new HashMap<>();
        
        // 先添加工作流定义中的全局变量
        if (definition.getGlobalVars() != null && !definition.getGlobalVars().isEmpty()) {
            for (Map.Entry<String, Object> entry : definition.getGlobalVars().entrySet()) {
                Object processedValue = processVariableValue(entry.getKey(), entry.getValue());
                variables.put(entry.getKey(), processedValue);
            }
        }
        
        // 将环境变量合并到 variables 中（不区分大小写匹配，用户变量会覆盖）
        if (profile != null && profile.getVariables() != null && !profile.getVariables().isEmpty()) {
            Map<String, Object> envVars = profile.getVariables();
            for (Map.Entry<String, Object> entry : envVars.entrySet()) {
                String key = entry.getKey();
                // 检查用户变量中是否有相同key（不区分大小写）
                boolean foundInUserVars = false;
                if (userVariables != null && !userVariables.isEmpty()) {
                    for (String userKey : userVariables.keySet()) {
                        if (key.equalsIgnoreCase(userKey)) {
                            foundInUserVars = true;
                            break;
                        }
                    }
                }
                // 如果用户变量中没有，才添加环境变量
                if (!foundInUserVars) {
                    Object processedValue = processVariableValue(key, entry.getValue());
                    variables.put(key, processedValue);
                }
            }
        }
        
        // 将环境配置的结构化数据扁平化到执行机的 variables 中（不再使用嵌套结构）
        if (profile != null) {
            // 1. HTTP节点：domain -> variables.url（扁平化）
            if (StringUtils.isNotBlank(profile.getDomain())) {
                if (!variables.containsKey("url")) {
                    variables.put("url", profile.getDomain());
                }
            }
            
            // 2. SQL节点：dataEndpoint 扁平化到 variables（字段重命名：host->data_host, port->data_port, user->data_user, password->data_password）
            if (profile.getDataEndpoint() != null && !profile.getDataEndpoint().isEmpty()) {
                Map<String, Object> dataEndpoint = profile.getDataEndpoint();
                
                Object hostValue = dataEndpoint.get("data_host");
                if (hostValue != null && !variables.containsKey("data_host")) {
                    Object processedValue = processVariableValue("data_host", hostValue);
                    variables.put("data_host", processedValue);
                }
                
                Object portValue = dataEndpoint.get("data_port");
                if (portValue != null && !variables.containsKey("data_port")) {
                    Object processedValue = processVariableValue("data_port", portValue);
                    variables.put("data_port", processedValue);
                }
                
                Object userValue = dataEndpoint.get("data_user");
                if (userValue != null && !variables.containsKey("data_user")) {
                    Object processedValue = processVariableValue("data_user", userValue);
                    variables.put("data_user", processedValue);
                }
                
                Object passwordValue = dataEndpoint.get("data_password");
                if (passwordValue != null && !variables.containsKey("data_password")) {
                    Object processedValue = processVariableValue("data_password", passwordValue);
                    variables.put("data_password", processedValue);
                }
            }
            
            // 3. MQ节点：mqInfo.mq_url -> variables.mq_url（扁平化）
            if (profile.getMqInfo() != null && !profile.getMqInfo().isEmpty()) {
                Object mqUrl = profile.getMqInfo().get("mq_url");
                if (mqUrl != null && !variables.containsKey("mq_url")) {
                    Object processedValue = processVariableValue("mq_url", mqUrl);
                    variables.put("mq_url", processedValue);
                }
            }
            
            // 4. Dubbo节点：dubboInfo.dubbo_url -> variables.dubbo_url（扁平化）
            if (profile.getDubboInfo() != null && !profile.getDubboInfo().isEmpty()) {
                Object dubboUrl = profile.getDubboInfo().get("dubbo_url");
                if (dubboUrl != null && !variables.containsKey("dubbo_url")) {
                    Object processedValue = processVariableValue("dubbo_url", dubboUrl);
                    variables.put("dubbo_url", processedValue);
                }
            }
            
            // 5. XXL-Job节点：xxljobInfo 扁平化到 variables（字段重命名：user->xxljobuser, password->xxljobpassword, xxjob_url保持不变）
            if (profile.getXxljobInfo() != null && !profile.getXxljobInfo().isEmpty()) {
                Map<String, Object> xxljobInfo = profile.getXxljobInfo();
                
                Object xxjobUrlValue = xxljobInfo.get("xxjob_url");
                if (xxjobUrlValue != null && !variables.containsKey("xxjob_url")) {
                    Object processedValue = processVariableValue("xxjob_url", xxjobUrlValue);
                    variables.put("xxjob_url", processedValue);
                }
                
                Object userValue = xxljobInfo.get("xxljobuser");
                if (userValue != null && !variables.containsKey("xxljobuser")) {
                    Object processedValue = processVariableValue("xxljobuser", userValue);
                    variables.put("xxljobuser", processedValue);
                }
                
                Object passwordValue = xxljobInfo.get("xxljobpassword");
                if (passwordValue != null && !variables.containsKey("xxljobpassword")) {
                    Object processedValue = processVariableValue("xxljobpassword", passwordValue);
                    variables.put("xxljobpassword", processedValue);
                }
            }
        }
        
        // 注意：不再使用嵌套结构，所有变量都扁平化到 variables 中
        // 节点的 environmentVariables 已经在 processNodeConfig 中处理，这里不再需要额外合并
        
        // 最后添加用户输入的变量（优先级最高，覆盖环境变量和节点environmentVariables）
        if (userVariables != null && !userVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : userVariables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (StringUtils.isNotBlank(value)) {
                    // 检查variables中是否已有相同key（不区分大小写），如果有则替换
                    String existingKey = null;
                    for (String varKey : variables.keySet()) {
                        if (key.equalsIgnoreCase(varKey)) {
                            existingKey = varKey;
                            break;
                        }
                    }
                    if (existingKey != null) {
                        variables.remove(existingKey);
                    }
                    Object processedValue = processVariableValue(key, value);
                    variables.put(key, processedValue);
                }
            }
        }
        
        // 构建 workflows 数组中的 workflow 对象
        Map<String, Object> workflowItem = new HashMap<>();
        workflowItem.put("workflow", workflowObj);
        workflowItem.put("variables", variables);
        workflowItem.put("runId", runId);
        
        return workflowItem;
    }

    /**
     * 处理HTTP节点的headers：格式转换 + 用户输入变量的占位符处理
     * 如果用户在环境选择弹窗中输入了 X-Tag-Header、X-Site-Tenant、X-Tenant-Id、X-App 的值，
     * 且 HTTP 节点的 headers 中存在对应的 header key，则将 header 的 value 改为变量占位符格式
     * 变量替换由执行机负责
     * @param config HTTP节点配置
     * @param userVariables 用户输入的变量（从环境选择弹窗中获取）
     * @param profile 环境配置（不再使用，保留参数以保持接口兼容）
     */
    private void processHttpHeaders(Map<String, Object> config, Map<String, String> userVariables, WorkflowEngineProfile profile) {
        if (config == null) {
            return;
        }
        
        Object headersObj = config.get("headers");
        if (headersObj == null) {
            return;
        }
        
        // headers可能是Map或List格式，需要转换为Map格式（执行机期望Map格式）
        Map<String, Object> headersMap = null;
        if (headersObj instanceof Map) {
            headersMap = new HashMap<>((Map<String, Object>) headersObj);
        } else if (headersObj instanceof List) {
            // 如果是List格式，转换为Map
            headersMap = new HashMap<>();
            List<?> headersList = (List<?>) headersObj;
            for (Object item : headersList) {
                if (item instanceof Map) {
                    Map<?, ?> headerItem = (Map<?, ?>) item;
                    Object key = headerItem.get("key");
                    Object value = headerItem.get("value");
                    if (key != null) {
                        headersMap.put(String.valueOf(key), value);
                    }
                }
            }
        } else {
            return;
        }
        
        if (headersMap == null || headersMap.isEmpty()) {
            return;
        }
        
        // 需要处理的header key列表（不区分大小写）
        // 用户输入的变量key使用小写格式：x-tag-header, x-site-tenant, x-tenant-id, x-app
        String[] headerKeysToProcess = {"X-Tag-Header", "X-Site-Tenant", "X-Tenant-Id", "X-App"};
        Map<String, String> userVarKeyMap = new HashMap<>();
        userVarKeyMap.put("X-Tag-Header", "x-tag-header");
        userVarKeyMap.put("X-Site-Tenant", "x-site-tenant");
        userVarKeyMap.put("X-Tenant-Id", "x-tenant-id");
        userVarKeyMap.put("X-App", "x-app");
        
        // 检查用户输入的变量，如果用户输入了值，且 headers 中存在对应的 header key，则将 value 改为变量占位符格式
        if (userVariables != null && !userVariables.isEmpty()) {
            for (String headerKeyToProcess : headerKeysToProcess) {
                String userVarKey = userVarKeyMap.get(headerKeyToProcess);
                if (userVarKey != null && userVariables.containsKey(userVarKey)) {
                    String userValue = userVariables.get(userVarKey);
                    if (StringUtils.isNotBlank(userValue)) {
                        // 查找 headers 中是否存在该 key（不区分大小写）
                        String foundHeaderKey = null;
                        for (String headerKey : headersMap.keySet()) {
                            if (headerKeyToProcess.equalsIgnoreCase(headerKey)) {
                                foundHeaderKey = headerKey;
                                break;
                            }
                        }
                        
                        if (foundHeaderKey != null) {
                            // 将 header 的 value 改为变量占位符格式（使用小写的变量名）
                            String placeholder = "${" + userVarKey + "}";
                            // 如果原 header key 存在，先移除旧的（可能大小写不同）
                            headersMap.remove(foundHeaderKey);
                            // 使用标准的 header key（大写格式）添加占位符
                            headersMap.put(headerKeyToProcess, placeholder);
                            log.debug("处理HTTP headers - 用户输入了 {}，将 header {} 的值改为占位符: {}", userVarKey, headerKeyToProcess, placeholder);
                        }
                    }
                }
            }
        }
        
        // 更新config中的headers
        config.put("headers", headersMap);
    }

    /**
     * 统一的节点配置处理方法
     * 
     * 简化逻辑：用户写什么，我们传什么
     * - 不再做变量转换
     * - 如果用户需要使用变量，自己填写 $url 或 ${url} 格式
     * - 执行机负责解析变量
     * 
     * @param config 节点配置
     * @param nodeType 节点类型（执行机格式，如http_request、mysql、dubbo等）
     * @param userVariables 用户输入的变量
     * @param profile 环境配置
     * @return 处理结果，包含处理后的config和提取的environmentVariables
     */
    private NodeConfigProcessResult processNodeConfig(Map<String, Object> config, String nodeType, 
                                                      Map<String, String> userVariables, WorkflowEngineProfile profile) {
        NodeConfigProcessResult result = new NodeConfigProcessResult();
        result.config = config;
        result.environmentVariables = new HashMap<>();
        
        if (config == null) {
            return result;
        }
        
        // 1. 移除environmentId（不传递给执行机）
        config.remove("environmentId");
        
        // 2. 移除environmentVariables（不再需要，已简化为直接使用字段值）
        config.remove("environmentVariables");
        
        // 3. HTTP节点：处理headers（保留请求头处理逻辑）
        if ("http_request".equals(nodeType) || "http".equals(nodeType)) {
            processHttpHeaders(config, userVariables, profile);
        }
        
        // 4. SQL节点：处理connection.port字段，确保为整数类型
        if ("mysql".equals(nodeType) || "sql".equals(nodeType)) {
            Object connectionObj = config.get("connection");
            if (connectionObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> connection = (Map<String, Object>) connectionObj;
                Object portObj = connection.get("port");
                if (portObj != null) {
                    // 如果是字符串，且为变量占位符（如 ${data_port}）则保持原样由执行机解析，否则转为整数
                    if (portObj instanceof String) {
                        String portStr = ((String) portObj).trim();
                        if (portStr.startsWith("${") && portStr.endsWith("}")) {
                            // 变量占位符不转换，执行机用 globalVariables 解析
                            connection.put("port", portStr);
                        } else {
                            try {
                                connection.put("port", Integer.parseInt(portStr));
                            } catch (NumberFormatException e) {
                                log.warn("SQL节点port字段转换int失败: {}", portStr);
                            }
                        }
                    } else if (portObj instanceof Number) {
                        // 如果已经是数字，确保是Integer类型
                        connection.put("port", portObj instanceof Integer ? portObj : ((Number) portObj).intValue());
                    }
                }
            }
        }
        
        // 注意：不再做变量转换，用户填写什么就传什么
        // 如果用户填写了 $mq_url 或 ${mq_url}，执行机负责解析
        
        return result;
    }
    
    /**
     * 节点配置处理结果
     */
    private static class NodeConfigProcessResult {
        Map<String, Object> config;
        Map<String, Object> environmentVariables;
    }
    
    /**
     * 节点环境变量及其类型（用于构建嵌套结构的 globalVariables）
     */
    private static class NodeEnvVarsWithType {
        String nodeType;
        Map<String, Object> environmentVariables;
    }
    
    /**
     * 根据节点类型获取嵌套结构名
     * @param nodeType 节点类型
     * @return 嵌套结构名（如 mqInfo, dataEndpoint 等），如果无法确定则返回 null
     */
    private String getNestedKeyForNodeType(String nodeType) {
        if (nodeType == null) {
            return null;
        }
        switch (nodeType.toLowerCase()) {
            case "http_request":
            case "http":
                return "httpConfig";
            case "rocketmq":
            case "mq":
                return "mqInfo";
            case "mysql":
            case "sql":
                return "dataEndpoint";
            case "dubbo":
                return "dubboInfo";
            case "xxl_job":
            case "xxljob":
                return "xxljobInfo";
            default:
                return null;
        }
    }
    
    /**
     * 处理变量值：如果是port字段且为字符串，转换为int
     * @param key 变量名
     * @param value 变量值
     * @return 处理后的值
     */
    private Object processVariableValue(String key, Object value) {
        if (value == null) {
            return value;
        }
        
        // 如果是port字段且为字符串，转换为int
        if ("port".equalsIgnoreCase(key)) {
            if (value instanceof String) {
                String portStr = ((String) value).trim();
                try {
                    return Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    log.warn("port变量值转换int失败: {}", portStr);
                    return value;
                }
            }
        }
        
        return value;
    }
    
    /**
     * 处理RocketMQ节点配置：只做清理，不做变量替换
     * 变量替换由执行机负责
     * @param config 节点配置
     * @param nodeEnvironmentVariables 不再使用，保留参数以保持接口兼容
     * @param envVarsMap 不再使用，保留参数以保持接口兼容
     * @param userVariables 不再使用，保留参数以保持接口兼容
     */
    private void processRocketMQConfig(Map<String, Object> config, Map<String, Object> nodeEnvironmentVariables,
                                       Map<String, Object> envVarsMap, Map<String, String> userVariables) {
        // 不做任何变量替换，直接传递用户输入的值给执行机
        // 只做必要的清理工作
        if (config == null) {
            return;
        }
        // 移除不需要的字段
        config.remove("environmentId");
        config.remove("environmentVariables");
    }
    
    /**
     * 处理MySQL节点配置：只做清理和类型转换，不做变量替换
     * 变量替换由执行机负责
     * @param config 节点配置
     * @param nodeEnvironmentVariables 不再使用，保留参数以保持接口兼容
     * @param envVarsMap 不再使用，保留参数以保持接口兼容
     * @param userVariables 不再使用，保留参数以保持接口兼容
     */
    private void processMySQLConfig(Map<String, Object> config, Map<String, Object> nodeEnvironmentVariables,
                                    Map<String, Object> envVarsMap, Map<String, String> userVariables) {
        if (config == null) {
            return;
        }
        
        // 处理connection对象
        Object connectionObj = config.get("connection");
        if (connectionObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) connectionObj;
            
            // 移除不需要的字段
            connection.remove("environmentId");
            
            // 只做类型转换：变量占位符（如 ${data_port}）保持原样，否则转为整数
            Object portObj = connection.get("port");
            if (portObj != null) {
                if (portObj instanceof String) {
                    String portStr = ((String) portObj).trim();
                    if (portStr.startsWith("${") && portStr.endsWith("}")) {
                        connection.put("port", portStr);
                    } else {
                        try {
                            connection.put("port", Integer.parseInt(portStr));
                        } catch (NumberFormatException e) {
                            log.warn("SQL节点port字段转换int失败: {}", portStr);
                        }
                    }
                } else if (portObj instanceof Number) {
                    connection.put("port", portObj instanceof Integer ? portObj : ((Number) portObj).intValue());
                }
            }
        }
        
        // 移除不需要的字段
        config.remove("environmentId");
        config.remove("environmentVariables");
    }
    
    /**
     * 处理Dubbo节点配置：只做清理，不做变量替换
     * 变量替换由执行机负责
     * @param config 节点配置
     * @param nodeEnvironmentVariables 不再使用，保留参数以保持接口兼容
     * @param envVarsMap 不再使用，保留参数以保持接口兼容
     * @param userVariables 不再使用，保留参数以保持接口兼容
     */
    private void processDubboConfig(Map<String, Object> config, Map<String, Object> nodeEnvironmentVariables,
                                    Map<String, Object> envVarsMap, Map<String, String> userVariables) {
        // 不做任何变量替换，直接传递用户输入的值给执行机
        // 只做必要的清理工作
        if (config == null) {
            return;
        }
        // 移除不需要的字段
        config.remove("environmentId");
        config.remove("environmentVariables");
    }
    
    /**
     * 处理XXL_JOB节点配置：只做清理，不做变量替换
     * 变量替换由执行机负责
     * @param config 节点配置
     * @param nodeEnvironmentVariables 不再使用，保留参数以保持接口兼容
     * @param envVarsMap 不再使用，保留参数以保持接口兼容
     * @param userVariables 不再使用，保留参数以保持接口兼容
     */
    private void processXxlJobConfig(Map<String, Object> config, Map<String, Object> nodeEnvironmentVariables,
                                     Map<String, Object> envVarsMap, Map<String, String> userVariables) {
        // 不做任何变量替换，直接传递用户输入的值给执行机
        // 只做必要的清理工作
        if (config == null) {
            return;
        }
        // 移除不需要的字段
        config.remove("environmentId");
        config.remove("environmentVariables");
    }
}
