package io.vanguard.testops.workflow.service;

import io.vanguard.testops.metadata.domain.MetadataModule;
import io.vanguard.testops.metadata.mapper.MetadataModuleMapper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.workflow.domain.WorkflowDefinition;
import io.vanguard.testops.workflow.domain.WorkflowStep;
import io.vanguard.testops.workflow.domain.WorkflowStepLink;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.mapper.WorkflowDefinitionMapper;
import io.vanguard.testops.workflow.mapper.WorkflowStepLinkMapper;
import io.vanguard.testops.workflow.mapper.WorkflowStepMapper;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流定义服务
 * 核心功能：保存和加载工作流（包含节点坐标和连线样式）
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowDefinitionService {

    @Resource
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Resource
    private WorkflowStepMapper workflowStepMapper;

    @Resource
    private WorkflowStepLinkMapper workflowStepLinkMapper;

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    @Resource
    private MetadataModuleMapper metadataModuleMapper;

    /**
     * ⭐ 核心方法：保存工作流（创建或更新）
     * 同时保存工作流定义、步骤列表（包含坐标）、连线列表（包含样式）
     */
    public String save(WorkflowDefinitionSaveRequest request, String userId) {
        long currentTime = System.currentTimeMillis();
        String workflowId;
        boolean isUpdate = StringUtils.isNotBlank(request.getWorkflowId());

        if (isUpdate) {
            // 更新已存在的工作流
            workflowId = request.getWorkflowId();
            WorkflowDefinition existing = workflowDefinitionMapper.selectByWorkflowId(workflowId);
            if (existing == null) {
                throw new MSException("工作流不存在");
            }

            // 更新工作流定义
            WorkflowDefinition definition = buildWorkflowDefinition(request, userId, currentTime, false);
            definition.setWorkflowId(workflowId);
            definition.setCreateUser(existing.getCreateUser());
            definition.setCreateTime(existing.getCreateTime());
            definition.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 1);
            workflowDefinitionMapper.updateById(definition);

            // 更新时对节点做 UPSERT：已存在的节点 UPDATE，新增的 INSERT，被移除的 DELETE；不再整体先删后插，避免误删或空请求导致数据丢失
            if (CollectionUtils.isNotEmpty(request.getNodes())) {
                upsertStepsAndLinks(workflowId, request, currentTime);
            }

        } else {
            // 创建新工作流
            workflowId = IDGenerator.nextStr();
            WorkflowDefinition definition = buildWorkflowDefinition(request, userId, currentTime, true);
            definition.setWorkflowId(workflowId);
            definition.setCreateUser(userId);
            definition.setCreateTime(currentTime);
            definition.setVersion(1);
            workflowDefinitionMapper.insert(definition);
        }

        // 保存步骤列表与连线（创建时整批插入，更新时在 upsertStepsAndLinks 中已处理，此处仅处理创建）
        Map<String, String> stepIdMapping = new HashMap<>();
        if (CollectionUtils.isNotEmpty(request.getNodes())) {
            if (!isUpdate) {
                List<WorkflowStep> steps = buildWorkflowSteps(request.getNodes(), workflowId, currentTime, stepIdMapping);
                if (!steps.isEmpty()) {
                    workflowStepMapper.batchInsert(steps);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(request.getConnections()) && !isUpdate) {
            List<WorkflowStepLink> links = buildWorkflowStepLinks(request.getConnections(), workflowId, currentTime, stepIdMapping);
            if (!links.isEmpty()) {
                workflowStepLinkMapper.batchInsert(links);
            }
        }

        return workflowId;
    }

    /**
     * ⭐ 核心方法：获取工作流详情
     * 加载工作流定义、步骤列表（恢复坐标）、连线列表（恢复样式）
     */
    public WorkflowDefinitionDTO get(String workflowId) {
        // 1. 加载工作流定义
        WorkflowDefinition definition = workflowDefinitionMapper.selectByWorkflowId(workflowId);
        if (definition == null) {
            throw new MSException("工作流不存在");
        }

        // 2. 加载步骤列表（恢复坐标）
        List<WorkflowStep> steps = workflowStepMapper.selectByWorkflowId(workflowId);
        List<WorkflowNodeDTO> nodes = steps.stream()
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());

        // 3. 加载连线列表（恢复样式）
        List<WorkflowStepLink> links = workflowStepLinkMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionDTO> connections = links.stream()
                .map(this::convertToConnectionDTO)
                .collect(Collectors.toList());

        // 4. 组装返回对象
        return buildWorkflowDefinitionDTO(definition, nodes, connections);
    }

    /**
     * 分页查询工作流列表
     */
    public List<WorkflowDefinitionListItem> list(WorkflowDefinitionPageRequest request) {
        List<WorkflowDefinition> definitions;
        
        if (StringUtils.isNotBlank(request.getModuleId())) {
            // 如果指定了模块ID，查询该模块下的工作流
            definitions = workflowDefinitionMapper.selectByModuleId(request.getModuleId());
        } else if (StringUtils.isNotBlank(request.getWorkspaceId())) {
            // 如果指定了工作空间ID，查询该空间下所有模块的工作流（一次批量查询，避免 N 次 selectByModuleId）
            List<MetadataModule> modules = metadataModuleMapper.selectByProjectIdAndTypeId(
                    request.getProjectId(), request.getWorkspaceId(), "WORKFLOW");
            if (CollectionUtils.isEmpty(modules)) {
                definitions = new ArrayList<>();
            } else {
                List<String> moduleIds = modules.stream()
                        .map(MetadataModule::getId)
                        .collect(Collectors.toList());
                definitions = workflowDefinitionMapper.selectByModuleIds(moduleIds);
            }
        } else {
            // 如果都没有指定，查询整个项目下的工作流
            definitions = workflowDefinitionMapper.selectByProjectId(request.getProjectId());
        }

        // 批量查询步骤数与最新运行记录，避免 N+1
        List<String> workflowIds = definitions.stream().map(WorkflowDefinition::getWorkflowId).collect(Collectors.toList());
        final Map<String, Integer> stepCountByWorkflowId;
        final Map<String, WorkflowRun> latestRunByWorkflowId;
        if (!workflowIds.isEmpty()) {
            List<WorkflowStep> allSteps = workflowStepMapper.selectByWorkflowIds(workflowIds);
            stepCountByWorkflowId = allSteps.stream()
                    .collect(Collectors.groupingBy(WorkflowStep::getWorkflowId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
            Map<String, WorkflowRun> latestMap = new HashMap<>();
            try {
                List<WorkflowRun> allRuns = workflowRunMapper.selectByWorkflowIds(workflowIds);
                for (WorkflowRun r : allRuns) {
                    latestMap.putIfAbsent(r.getWorkflowId(), r);
                }
            } catch (Exception e) {
                // 执行历史查询失败不影响列表
            }
            latestRunByWorkflowId = latestMap;
        } else {
            stepCountByWorkflowId = new HashMap<>();
            latestRunByWorkflowId = new HashMap<>();
        }

        // 转换为列表项
        return definitions.stream().map(def -> {
            WorkflowDefinitionListItem item = new WorkflowDefinitionListItem();
            item.setWorkflowId(def.getWorkflowId());
            item.setName(def.getName());
            item.setDescription(def.getDescription());
            item.setCategory(def.getCategory());
            item.setType(def.getType());
            item.setVersion(def.getVersion());
            item.setStatus(def.getStatus());
            item.setCreateUser(def.getCreateUser());
            item.setCreateTime(def.getCreateTime());
            item.setUpdateTime(def.getUpdateTime());
            item.setStepCount(stepCountByWorkflowId.getOrDefault(def.getWorkflowId(), 0));
            WorkflowRun latestRun = latestRunByWorkflowId.get(def.getWorkflowId());
            if (latestRun != null) {
                item.setLastDurationMs(latestRun.getDurationMs());
                item.setLastRunStatus(latestRun.getStatus());
                item.setLastRunTime(latestRun.getStartTime() != null ? latestRun.getStartTime() : latestRun.getCreateTime());
            }
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 删除工作流（软删除）
     * 同时软删除关联的步骤和连线
     */
    public void delete(String workflowId, String userId) {
        WorkflowDefinition existing = workflowDefinitionMapper.selectByWorkflowId(workflowId);
        if (existing == null) {
            throw new MSException("工作流不存在");
        }

        long currentTime = System.currentTimeMillis();
        LocalDateTime deletedTime = LocalDateTime.now();

        // 软删除工作流定义
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setWorkflowId(workflowId);
        definition.setDeletedTime(deletedTime);
        definition.setUpdateTime(currentTime);
        definition.setUpdateUser(userId);
        workflowDefinitionMapper.updateById(definition);

        // 软删除所有关联的步骤
        List<WorkflowStep> steps = workflowStepMapper.selectByWorkflowId(workflowId);
        if (CollectionUtils.isNotEmpty(steps)) {
            for (WorkflowStep step : steps) {
                WorkflowStep updateStep = new WorkflowStep();
                updateStep.setStepId(step.getStepId());
                updateStep.setDeletedTime(deletedTime);
                updateStep.setUpdateTime(currentTime);
                workflowStepMapper.updateById(updateStep);
            }
        }

        // 软删除所有关联的连线
        List<WorkflowStepLink> links = workflowStepLinkMapper.selectByWorkflowId(workflowId);
        if (CollectionUtils.isNotEmpty(links)) {
            for (WorkflowStepLink link : links) {
                WorkflowStepLink updateLink = new WorkflowStepLink();
                updateLink.setLinkId(link.getLinkId());
                updateLink.setDeletedTime(deletedTime);
                updateLink.setUpdateTime(currentTime);
                workflowStepLinkMapper.updateById(updateLink);
            }
        }
    }

    /**
     * 复制工作流
     */
    public String copy(String workflowId, String userId) {
        // 获取原工作流详情
        WorkflowDefinitionDTO source = get(workflowId);

        // 创建新工作流
        WorkflowDefinitionSaveRequest request = new WorkflowDefinitionSaveRequest();
        request.setProjectId(source.getProjectId());
        request.setModuleId(source.getModuleId());
        request.setName(source.getName() + "_copy");
        request.setDescription(source.getDescription());
        request.setCategory(source.getCategory());
        request.setType(source.getType());
        request.setGlobalVars(source.getGlobalVars());

        // 为节点生成新的ID，并建立旧ID到新ID的映射
        Map<String, String> idMapping = new HashMap<>();
        List<WorkflowNodeDTO> newNodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(source.getNodes())) {
            for (WorkflowNodeDTO node : source.getNodes()) {
                String oldId = node.getId();
                String newId = IDGenerator.nextStr();
                idMapping.put(oldId, newId);
                
                // 创建新节点，使用新ID
                WorkflowNodeDTO newNode = new WorkflowNodeDTO();
                newNode.setId(newId);
                newNode.setType(node.getType());
                newNode.setName(node.getName());
                newNode.setDescription(node.getDescription());
                newNode.setConfig(node.getConfig());
                newNode.setX(node.getX());
                newNode.setY(node.getY());
                newNode.setOrderNum(node.getOrderNum());
                // 复制工作流时，如果源节点的 refMode 是 'REF_METADATA'，应该改为 'COPY'
                // 但保留 refMetadataId，以便后续可以验证元数据是否存在
                String refMode = node.getRefMode();
                if ("REF_METADATA".equals(refMode)) {
                    refMode = "COPY";
                }
                newNode.setRefMode(refMode);
                newNode.setRefMetadataId(node.getRefMetadataId());
                newNode.setRefWorkflowId(node.getRefWorkflowId());
                newNode.setEnable(node.getEnable());
                newNodes.add(newNode);
            }
        }
        request.setNodes(newNodes);

        // 更新连线中的节点引用
        List<WorkflowConnectionDTO> newConnections = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(source.getConnections())) {
            for (WorkflowConnectionDTO conn : source.getConnections()) {
                WorkflowConnectionDTO newConn = new WorkflowConnectionDTO();
                newConn.setFrom(idMapping.getOrDefault(conn.getFrom(), conn.getFrom()));
                newConn.setTo(idMapping.getOrDefault(conn.getTo(), conn.getTo()));
                newConn.setLabel(conn.getLabel());
                newConn.setColor(conn.getColor());
                newConn.setConditionExpr(conn.getConditionExpr());
                newConn.setOrderNum(conn.getOrderNum());
                newConnections.add(newConn);
            }
        }
        request.setConnections(newConnections);

        return save(request, userId);
    }

    /**
     * 批量复制工作流到指定模块
     */
    public List<String> batchCopy(WorkflowBatchCopyRequest request, String userId) {
        if (CollectionUtils.isEmpty(request.getWorkflowIds())) {
            throw new MSException("工作流ID列表不能为空");
        }
        if (StringUtils.isBlank(request.getTargetModuleId())) {
            throw new MSException("目标模块ID不能为空");
        }

        // 验证目标模块是否存在
        MetadataModule targetModule = metadataModuleMapper.selectById(request.getTargetModuleId());
        if (targetModule == null) {
            throw new MSException("目标模块不存在");
        }

        List<String> newWorkflowIds = new ArrayList<>();
        for (String workflowId : request.getWorkflowIds()) {
            try {
                // 获取原工作流详情
                WorkflowDefinitionDTO source = get(workflowId);
                if (source == null) {
                    continue; // 跳过不存在的工作流
                }

                // 创建新工作流
                WorkflowDefinitionSaveRequest saveRequest = new WorkflowDefinitionSaveRequest();
                saveRequest.setProjectId(source.getProjectId());
                saveRequest.setModuleId(request.getTargetModuleId()); // 使用目标模块ID
                saveRequest.setName(source.getName() + "_copy");
                saveRequest.setDescription(source.getDescription());
                saveRequest.setCategory(source.getCategory());
                saveRequest.setType(source.getType());
                saveRequest.setGlobalVars(source.getGlobalVars());

                // 为节点生成新的ID，并建立旧ID到新ID的映射
                Map<String, String> idMapping = new HashMap<>();
                List<WorkflowNodeDTO> newNodes = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(source.getNodes())) {
                    for (WorkflowNodeDTO node : source.getNodes()) {
                        String oldId = node.getId();
                        String newId = IDGenerator.nextStr();
                        idMapping.put(oldId, newId);
                        
                        // 创建新节点，使用新ID
                        WorkflowNodeDTO newNode = new WorkflowNodeDTO();
                        newNode.setId(newId);
                        newNode.setType(node.getType());
                        newNode.setName(node.getName());
                        newNode.setDescription(node.getDescription());
                        newNode.setConfig(node.getConfig());
                        newNode.setX(node.getX());
                        newNode.setY(node.getY());
                        newNode.setOrderNum(node.getOrderNum());
                        // 复制工作流时，如果源节点的 refMode 是 'REF_METADATA'，应该改为 'COPY'
                        String refMode = node.getRefMode();
                        if ("REF_METADATA".equals(refMode)) {
                            refMode = "COPY";
                        }
                        newNode.setRefMode(refMode);
                        newNode.setRefMetadataId(node.getRefMetadataId());
                        newNode.setRefWorkflowId(node.getRefWorkflowId());
                        newNode.setEnable(node.getEnable());
                        newNodes.add(newNode);
                    }
                }
                saveRequest.setNodes(newNodes);

                // 更新连线中的节点引用
                List<WorkflowConnectionDTO> newConnections = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(source.getConnections())) {
                    for (WorkflowConnectionDTO conn : source.getConnections()) {
                        WorkflowConnectionDTO newConn = new WorkflowConnectionDTO();
                        newConn.setFrom(idMapping.getOrDefault(conn.getFrom(), conn.getFrom()));
                        newConn.setTo(idMapping.getOrDefault(conn.getTo(), conn.getTo()));
                        newConn.setLabel(conn.getLabel());
                        newConn.setColor(conn.getColor());
                        newConn.setConditionExpr(conn.getConditionExpr());
                        newConn.setOrderNum(conn.getOrderNum());
                        newConnections.add(newConn);
                    }
                }
                saveRequest.setConnections(newConnections);

                // 保存新工作流
                String newWorkflowId = save(saveRequest, userId);
                newWorkflowIds.add(newWorkflowId);
            } catch (Exception e) {
                // 记录错误但继续处理其他工作流
                // 可以根据需要记录日志
                continue;
            }
        }

        return newWorkflowIds;
    }

    /**
     * 批量移动工作流到指定模块
     */
    public void batchMove(WorkflowBatchMoveRequest request, String userId) {
        if (CollectionUtils.isEmpty(request.getWorkflowIds())) {
            throw new MSException("工作流ID列表不能为空");
        }
        if (StringUtils.isBlank(request.getTargetModuleId())) {
            throw new MSException("目标模块ID不能为空");
        }

        // 验证目标模块是否存在
        MetadataModule targetModule = metadataModuleMapper.selectById(request.getTargetModuleId());
        if (targetModule == null) {
            throw new MSException("目标模块不存在");
        }

        long currentTime = System.currentTimeMillis();
        for (String workflowId : request.getWorkflowIds()) {
            try {
                // 获取工作流定义
                WorkflowDefinition definition = workflowDefinitionMapper.selectByWorkflowId(workflowId);
                if (definition == null) {
                    continue; // 跳过不存在的工作流
                }

                // 更新模块ID
                definition.setModuleId(request.getTargetModuleId());
                definition.setUpdateTime(currentTime);
                definition.setUpdateUser(userId);
                workflowDefinitionMapper.updateById(definition);
            } catch (Exception e) {
                // 记录错误但继续处理其他工作流
                // 可以根据需要记录日志
                continue;
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建工作流定义实体
     */
    private WorkflowDefinition buildWorkflowDefinition(WorkflowDefinitionSaveRequest request, 
                                                        String userId, long currentTime, boolean isNew) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setProjectId(request.getProjectId());
        definition.setModuleId(request.getModuleId());
        definition.setName(request.getName());
        definition.setDescription(request.getDescription());
        definition.setCategory(StringUtils.defaultIfBlank(request.getCategory(), "API"));
        definition.setType(StringUtils.defaultIfBlank(request.getType(), "TEST_CASE"));
        definition.setGlobalVars(request.getGlobalVars());
        definition.setEnvironmentId(request.getEnvironmentId());
        definition.setStatus("DRAFT");
        definition.setUpdateTime(currentTime);
        definition.setUpdateUser(userId);
        return definition;
    }

    /**
     * 清理 HTTP 节点配置中多余的请求体字段
     * 根据 bodyType 只保留对应的字段，清除其他字段（body、json、data、params、upload、files）
     */
    private Map<String, Object> cleanHttpNodeConfig(Map<String, Object> config) {
        if (config == null) {
            return new HashMap<>();
        }
        
        // 创建新的配置 Map，避免修改原始配置
        Map<String, Object> cleanedConfig = new HashMap<>(config);
        
        // 获取 bodyType，默认为 'json'
        String bodyType = (String) cleanedConfig.getOrDefault("bodyType", "json");
        
        // 根据 bodyType 清除其他类型的请求体字段
        if ("params".equals(bodyType)) {
            // 保留 params，清除其他
            cleanedConfig.remove("json");
            cleanedConfig.remove("data");
            cleanedConfig.remove("upload");
            cleanedConfig.remove("files");
            cleanedConfig.remove("body");
        } else if ("json".equals(bodyType)) {
            // 保留 json，清除其他
            cleanedConfig.remove("params");
            cleanedConfig.remove("data");
            cleanedConfig.remove("upload");
            cleanedConfig.remove("files");
            cleanedConfig.remove("body");
        } else if ("data".equals(bodyType)) {
            // 保留 data，清除其他
            cleanedConfig.remove("params");
            cleanedConfig.remove("json");
            cleanedConfig.remove("upload");
            cleanedConfig.remove("files");
            cleanedConfig.remove("body");
        } else if ("upload".equals(bodyType)) {
            // upload 模式：保留 upload/files 和 data（data 为 multipart 的文本字段），清除其他
            cleanedConfig.remove("params");
            cleanedConfig.remove("json");
            cleanedConfig.remove("body");
            // upload 和 files 是同一个字段的别名，确保两者都存在
            if (cleanedConfig.containsKey("upload") && !cleanedConfig.containsKey("files")) {
                cleanedConfig.put("files", cleanedConfig.get("upload"));
            } else if (cleanedConfig.containsKey("files") && !cleanedConfig.containsKey("upload")) {
                cleanedConfig.put("upload", cleanedConfig.get("files"));
            }
        } else {
            // 如果 bodyType 未知或未设置，默认清除所有请求体字段（除了 bodyType）
            // 这样可以避免保存无效的配置
            cleanedConfig.remove("params");
            cleanedConfig.remove("json");
            cleanedConfig.remove("data");
            cleanedConfig.remove("upload");
            cleanedConfig.remove("files");
            cleanedConfig.remove("body");
        }
        
        return cleanedConfig;
    }

    /**
     * 更新场景下对步骤与连线做 UPSERT：已存在节点 UPDATE，新增节点 INSERT，被移除节点 DELETE；连线按最新关系重新插入。
     */
    private void upsertStepsAndLinks(String workflowId, WorkflowDefinitionSaveRequest request, long currentTime) {
        List<WorkflowStep> existingSteps = workflowStepMapper.selectByWorkflowId(workflowId);
        Map<String, WorkflowStep> existingByStepId = existingSteps.stream().collect(Collectors.toMap(WorkflowStep::getStepId, s -> s));
        Map<String, String> stepIdMapping = new HashMap<>();
        List<WorkflowStep> toInsert = new ArrayList<>();
        List<WorkflowNodeDTO> nodes = request.getNodes();

        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNodeDTO node = nodes.get(i);
            String nodeId = StringUtils.isNotBlank(node.getId()) ? node.getId() : ("temp-" + i);
            if (existingByStepId.containsKey(nodeId)) {
                WorkflowStep step = buildOneStepFromNode(node, workflowId, currentTime, nodeId, i);
                step.setCreateTime(existingByStepId.get(nodeId).getCreateTime());
                workflowStepMapper.updateById(step);
                stepIdMapping.put(nodeId, nodeId);
                existingByStepId.remove(nodeId);
            } else {
                String newStepId = IDGenerator.nextStr();
                WorkflowStep step = buildOneStepFromNode(node, workflowId, currentTime, newStepId, i);
                toInsert.add(step);
                stepIdMapping.put(nodeId, newStepId);
            }
        }
        if (!toInsert.isEmpty()) {
            workflowStepMapper.batchInsert(toInsert);
        }
        for (String removedStepId : existingByStepId.keySet()) {
            workflowStepMapper.deleteById(removedStepId);
        }
        workflowStepLinkMapper.deleteByWorkflowId(workflowId);
        if (CollectionUtils.isNotEmpty(request.getConnections())) {
            List<WorkflowStepLink> links = buildWorkflowStepLinks(request.getConnections(), workflowId, currentTime, stepIdMapping);
            if (!links.isEmpty()) {
                workflowStepLinkMapper.batchInsert(links);
            }
        }
    }

    /**
     * 根据单个节点 DTO 构建一条步骤记录（用于 INSERT 或 UPDATE）
     */
    private WorkflowStep buildOneStepFromNode(WorkflowNodeDTO node, String workflowId, long currentTime, String stepId, int orderIndex) {
        WorkflowStep step = new WorkflowStep();
        step.setStepId(stepId);
        step.setWorkflowId(workflowId);
        step.setName(node.getName());
        step.setStepType(node.getType() != null ? node.getType().toUpperCase() : "HTTP");
        step.setOrderNum(node.getOrderNum() != null ? node.getOrderNum() : (long) orderIndex);
        Map<String, Object> nodeConfig = node.getConfig() != null ? node.getConfig() : new HashMap<>();
        if ("HTTP_REQUEST".equalsIgnoreCase(step.getStepType()) || "HTTP".equalsIgnoreCase(step.getStepType())) {
            nodeConfig = cleanHttpNodeConfig(nodeConfig);
        }
        step.setStepConfig(nodeConfig);
        step.setPositionX(node.getX() != null ? BigDecimal.valueOf(node.getX()) : BigDecimal.ZERO);
        step.setPositionY(node.getY() != null ? BigDecimal.valueOf(node.getY()) : BigDecimal.ZERO);
        step.setRefMode(StringUtils.defaultIfBlank(node.getRefMode(), "NONE"));
        step.setRefMetadataId(node.getRefMetadataId());
        step.setRefWorkflowId(node.getRefWorkflowId());
        step.setEnable(node.getEnable() != null ? node.getEnable() : true);
        step.setCreateTime(currentTime);
        step.setUpdateTime(currentTime);
        return step;
    }

    /**
     * 构建步骤列表（包含坐标）- 用于创建场景，始终生成新 step_id
     * @param stepIdMapping 前端ID到数据库ID的映射（输出参数）
     */
    private List<WorkflowStep> buildWorkflowSteps(List<WorkflowNodeDTO> nodes, String workflowId, long currentTime, Map<String, String> stepIdMapping) {
        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNodeDTO node = nodes.get(i);
            String frontendId = StringUtils.isNotBlank(node.getId()) ? node.getId() : ("temp-" + i);
            String dbStepId = IDGenerator.nextStr();
            stepIdMapping.put(frontendId, dbStepId);
            steps.add(buildOneStepFromNode(node, workflowId, currentTime, dbStepId, i));
        }
        return steps;
    }

    /**
     * 构建连线列表（包含样式）
     * @param stepIdMapping 前端ID到数据库ID的映射，用于更新连线中的步骤引用
     */
    private List<WorkflowStepLink> buildWorkflowStepLinks(List<WorkflowConnectionDTO> connections, 
                                                           String workflowId, long currentTime, 
                                                           Map<String, String> stepIdMapping) {
        List<WorkflowStepLink> links = new ArrayList<>();
        
        for (int i = 0; i < connections.size(); i++) {
            WorkflowConnectionDTO conn = connections.get(i);
            WorkflowStepLink link = new WorkflowStepLink();
            
            link.setLinkId(IDGenerator.nextStr());
            link.setWorkflowId(workflowId);
            
            // ⭐ 使用ID映射将前端ID转换为数据库ID
            String sourceStepId = stepIdMapping.getOrDefault(conn.getFrom(), conn.getFrom());
            String targetStepId = stepIdMapping.getOrDefault(conn.getTo(), conn.getTo());
            link.setSourceStepId(sourceStepId);
            link.setTargetStepId(targetStepId);
            
            // ⭐ 保存样式
            link.setLabel(conn.getLabel());
            link.setColor(conn.getColor());
            
            link.setConditionExpr(conn.getConditionExpr());
            link.setOrderNum(conn.getOrderNum() != null ? conn.getOrderNum() : i);
            link.setCreateTime(currentTime);
            link.setUpdateTime(currentTime);
            
            links.add(link);
        }
        
        return links;
    }

    /**
     * 转换步骤实体为节点 DTO（恢复坐标）
     */
    private WorkflowNodeDTO convertToNodeDTO(WorkflowStep step) {
        WorkflowNodeDTO node = new WorkflowNodeDTO();
        node.setId(step.getStepId());
        node.setType(step.getStepType() != null ? step.getStepType().toLowerCase() : "http");
        node.setName(step.getName());
        node.setConfig(step.getStepConfig());
        
        // 从 config 中提取 description
        if (step.getStepConfig() != null && step.getStepConfig().containsKey("description")) {
            node.setDescription((String) step.getStepConfig().get("description"));
        }
        
        // ⭐ 恢复坐标
        node.setX(step.getPositionX() != null ? step.getPositionX().doubleValue() : 0.0);
        node.setY(step.getPositionY() != null ? step.getPositionY().doubleValue() : 0.0);
        
        node.setOrderNum(step.getOrderNum());
        node.setRefMode(step.getRefMode());
        node.setRefMetadataId(step.getRefMetadataId());
        node.setRefWorkflowId(step.getRefWorkflowId());
        node.setEnable(step.getEnable());
        
        return node;
    }

    /**
     * 转换连线实体为连线 DTO（恢复样式）
     */
    private WorkflowConnectionDTO convertToConnectionDTO(WorkflowStepLink link) {
        WorkflowConnectionDTO conn = new WorkflowConnectionDTO();
        conn.setFrom(link.getSourceStepId());
        conn.setTo(link.getTargetStepId());
        
        // ⭐ 恢复样式
        conn.setLabel(link.getLabel());
        conn.setColor(link.getColor());
        
        conn.setConditionExpr(link.getConditionExpr());
        conn.setOrderNum(link.getOrderNum());
        
        return conn;
    }

    /**
     * 构建工作流定义 DTO
     */
    private WorkflowDefinitionDTO buildWorkflowDefinitionDTO(WorkflowDefinition definition,
                                                              List<WorkflowNodeDTO> nodes,
                                                              List<WorkflowConnectionDTO> connections) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setWorkflowId(definition.getWorkflowId());
        dto.setProjectId(definition.getProjectId());
        dto.setModuleId(definition.getModuleId());
        dto.setName(definition.getName());
        dto.setDescription(definition.getDescription());
        dto.setCategory(definition.getCategory());
        dto.setType(definition.getType());
        dto.setVersion(definition.getVersion());
        dto.setGlobalVars(definition.getGlobalVars());
        dto.setEnvironmentId(definition.getEnvironmentId());
        dto.setScheduleConfig(definition.getScheduleConfig());
        dto.setStatus(definition.getStatus());
        dto.setNodes(nodes);
        dto.setConnections(connections);
        dto.setCreateUser(definition.getCreateUser());
        dto.setCreateTime(definition.getCreateTime());
        dto.setUpdateTime(definition.getUpdateTime());
        dto.setUpdateUser(definition.getUpdateUser());
        return dto;
    }
}

