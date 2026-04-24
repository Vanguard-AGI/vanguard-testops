package io.vanguard.testops.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vanguard.testops.metadata.domain.MetadataDefinition;
import io.vanguard.testops.metadata.domain.ScriptManage;
import io.vanguard.testops.metadata.dto.*;
import io.vanguard.testops.metadata.mapper.MetadataDefinitionMapper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 元数据定义服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MetadataDefinitionService {

    @Resource
    private MetadataDefinitionMapper metadataDefinitionMapper;

    @Resource
    private io.vanguard.testops.metadata.mapper.MetadataModuleMapper metadataModuleMapper;
    
    @Resource
    private MetadataModuleService metadataModuleService;
    
    @Resource
    private io.vanguard.testops.metadata.mapper.ScriptManageMapper scriptManageMapper;

    /**
     * 创建元数据定义
     */
    public String create(MetadataDefinitionAddRequest request, String userId) {
        String id = IDGenerator.nextStr();
        long currentTime = System.currentTimeMillis();

        String scriptContent = request.getScriptContent();
        String scriptId = null;
        
        if ("SCRIPT".equals(request.getProtocol()) && StringUtils.isNotBlank(scriptContent)) {
            scriptId = IDGenerator.nextStr();
            String scriptType = getScriptTypeFromProtocol(request.getProtocol(), scriptContent);
            
            ScriptManage scriptManage = new ScriptManage();
            scriptManage.setScriptId(scriptId);
            scriptManage.setScriptName(request.getName());
            scriptManage.setScriptType(scriptType);
            scriptManage.setScriptContent(scriptContent);
            scriptManage.setCreateTime(currentTime);
            scriptManage.setUpdateTime(currentTime);
            
            scriptManageMapper.insert(scriptManage);
        }

        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(id);
        definition.setProjectId(request.getProjectId());
        definition.setModuleId(request.getModuleId());
        definition.setName(request.getName());
        definition.setProtocol(request.getProtocol());
        definition.setVersion(1);
        definition.setIsLatest(true);
        definition.setDescription(request.getDescription());
        definition.setRequestConfig(request.getRequestConfig());
        definition.setResponseConfig(request.getResponseConfig());
        definition.setScriptContent(scriptId != null ? scriptId : scriptContent);
        definition.setTags(request.getTags());
        definition.setIsCase(request.getIsCase() != null ? request.getIsCase() : false);
        definition.setCreateUser(userId);
        definition.setCreateTime(currentTime);
        definition.setUpdateTime(currentTime);

        metadataDefinitionMapper.insert(definition);

        return id;
    }
    
    /**
     * 根据 protocol 获取 script_type，保持一致性
     */
    private String getScriptTypeFromProtocol(String protocol, String scriptContent) {
        if (StringUtils.isBlank(protocol)) {
            return "SCRIPT";
        }
        
        String upperProtocol = protocol.toUpperCase();
        
        if ("SQL".equals(upperProtocol)) {
            return "SQL";
        }
        
        if ("SCRIPT".equals(upperProtocol)) {
            return "SCRIPT";
        }
        
        return upperProtocol;
    }
    
    /**
     * 检测脚本类型（当 protocol 为 SCRIPT 时使用）
     */
    private String detectScriptType(String scriptContent) {
        if (StringUtils.isBlank(scriptContent)) {
            return "PYTHON";
        }
        
        String content = scriptContent.toLowerCase().trim();
        
        if (content.contains("#!/bin/bash") || content.contains("#!/bin/sh") || 
            content.contains("#!/usr/bin/env bash") || content.contains("#!/usr/bin/env sh")) {
            return "SHELL";
        }
        
        if (content.contains("import java") || content.contains("public class") || 
            content.contains("public static void main")) {
            return "JAVA";
        }
        
        if (content.contains("select ") || content.contains("insert ") || 
            content.contains("update ") || content.contains("delete ") ||
            content.contains("create table") || content.contains("alter table")) {
            return "SQL";
        }
        
        if (content.contains("import ") || content.contains("def ") || 
            content.contains("class ") || content.contains("if __name__")) {
            return "PYTHON";
        }
        
        return "PYTHON";
    }
    
    /**
     * 根据 script_id 获取脚本详情
     */
    public ScriptManage getScriptById(String scriptId) {
        ScriptManage scriptManage = scriptManageMapper.selectByIdWithTimestamp(scriptId);
        if (scriptManage == null) {
            throw new MSException("脚本不存在");
        }
        return scriptManage;
    }

    /**
     * 更新元数据定义
     */
    public String update(MetadataDefinitionUpdateRequest request, String userId) {
        MetadataDefinition existing = metadataDefinitionMapper.selectByIdWithTimestamp(request.getId());
        if (existing == null) {
            throw new MSException("元数据不存在");
        }

        long currentTime = System.currentTimeMillis();
        String scriptContent = request.getScriptContent();
        String scriptId = null;
        
        if ("SCRIPT".equals(existing.getProtocol()) && StringUtils.isNotBlank(scriptContent)) {
            String existingScriptContent = existing.getScriptContent();
            
            if (StringUtils.isNotBlank(existingScriptContent)) {
                ScriptManage existingScript = scriptManageMapper.selectByIdWithTimestamp(existingScriptContent);
                if (existingScript != null) {
                    scriptId = existingScriptContent;
                    String scriptType = getScriptTypeFromProtocol(existing.getProtocol(), scriptContent);
                    
                    ScriptManage scriptManage = new ScriptManage();
                    scriptManage.setScriptId(scriptId);
                    scriptManage.setScriptName(request.getName());
                    scriptManage.setScriptType(scriptType);
                    scriptManage.setScriptContent(scriptContent);
                    scriptManage.setUpdateTime(currentTime);
                    
                    scriptManageMapper.updateById(scriptManage);
                } else {
                    scriptId = IDGenerator.nextStr();
                    String scriptType = getScriptTypeFromProtocol(existing.getProtocol(), scriptContent);
                    
                    ScriptManage scriptManage = new ScriptManage();
                    scriptManage.setScriptId(scriptId);
                    scriptManage.setScriptName(request.getName());
                    scriptManage.setScriptType(scriptType);
                    scriptManage.setScriptContent(scriptContent);
                    scriptManage.setCreateTime(currentTime);
                    scriptManage.setUpdateTime(currentTime);
                    
                    scriptManageMapper.insert(scriptManage);
                }
            } else {
                scriptId = IDGenerator.nextStr();
                String scriptType = getScriptTypeFromProtocol(existing.getProtocol(), scriptContent);
                
                ScriptManage scriptManage = new ScriptManage();
                scriptManage.setScriptId(scriptId);
                scriptManage.setScriptName(request.getName());
                scriptManage.setScriptType(scriptType);
                scriptManage.setScriptContent(scriptContent);
                scriptManage.setCreateTime(currentTime);
                scriptManage.setUpdateTime(currentTime);
                
                scriptManageMapper.insert(scriptManage);
            }
        }

        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(request.getId());
        definition.setName(request.getName());
        if (StringUtils.isNotBlank(request.getModuleId())) {
            definition.setModuleId(request.getModuleId());
        }
        definition.setDescription(request.getDescription());
        definition.setTags(request.getTags());
        definition.setRequestConfig(request.getRequestConfig());
        definition.setResponseConfig(request.getResponseConfig());
        definition.setScriptContent(scriptId != null ? scriptId : scriptContent);
        if (request.getIsCase() != null) {
            definition.setIsCase(request.getIsCase());
        }
        definition.setCreateUser(userId);
        definition.setUpdateTime(currentTime);

        metadataDefinitionMapper.updateById(definition);

        return request.getId();
    }

    /**
     * 批量移动模块ID
     */
    public void batchMoveModule(MetadataDefinitionBatchMoveModuleRequest request, String userId) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new MSException("ID列表不能为空");
        }
        
        if (StringUtils.isBlank(request.getModuleId())) {
            throw new MSException("目标模块ID不能为空");
        }
        
        long currentTime = System.currentTimeMillis();
        List<MetadataDefinition> definitionsToUpdate = new ArrayList<>();
        
        for (String id : request.getIds()) {
            MetadataDefinition existing = metadataDefinitionMapper.selectByIdWithTimestamp(id);
            if (existing == null) {
                throw new MSException("元数据不存在: " + id);
            }
            
            MetadataDefinition definition = new MetadataDefinition();
            definition.setId(id);
            definition.setModuleId(request.getModuleId());
            definition.setUpdateTime(currentTime);
            
            definitionsToUpdate.add(definition);
        }
        
        for (MetadataDefinition definition : definitionsToUpdate) {
            metadataDefinitionMapper.updateById(definition);
        }
    }

    /**
     * 删除元数据定义(物理删除，设置 deleted_time)
     */
    public void delete(String id, String userId) {
        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(id);
        definition.setCreateUser(userId);
        definition.setDeletedTime(System.currentTimeMillis());

        metadataDefinitionMapper.updateById(definition);
    }

    /**
     * 获取元数据定义详情
     */
    public MetadataDefinitionDTO get(String id) {
        MetadataDefinition definition = metadataDefinitionMapper.selectByIdWithTimestamp(id);
        if (definition == null) {
            throw new MSException("元数据不存在");
        }

        MetadataDefinitionDTO dto = new MetadataDefinitionDTO();
        dto.setId(definition.getId());
        dto.setProjectId(definition.getProjectId());
        dto.setModuleId(definition.getModuleId());
        dto.setName(definition.getName());
        dto.setProtocol(definition.getProtocol());
        dto.setVersion(definition.getVersion());
        dto.setIsLatest(definition.getIsLatest());
        dto.setIsCase(definition.getIsCase());
        dto.setDescription(definition.getDescription());
        dto.setRequestConfig(definition.getRequestConfig());
        dto.setResponseConfig(definition.getResponseConfig());
        dto.setScriptContent(definition.getScriptContent());
        dto.setTags(definition.getTags());
        dto.setCreateUser(definition.getCreateUser());
        dto.setCreateTime(definition.getCreateTime());
        dto.setUpdateTime(definition.getUpdateTime());
        
        if (definition.getRequestConfig() != null) {
            dto.setRequestConfigJson(JSON.toJSONString(definition.getRequestConfig()));
        }
        if (definition.getResponseConfig() != null) {
            dto.setResponseConfigJson(JSON.toJSONString(definition.getResponseConfig()));
        }

        return dto;
    }

    /** Aegis 脚本执行接口 base URL，可后续改为配置项 */
    private static final String AEGIS_BASE_URL = "http://aegis-ones-web.spotter.ink/spotter-data-forge";
    private static final String AEGIS_SCRIPT_TEST_ASYNC_URL = AEGIS_BASE_URL + "/project/script/test-async";
    private static final String AEGIS_RUN_RESULT_URL_TEMPLATE = AEGIS_BASE_URL + "/data/code/run-result/%s";

    /** 轮询执行结果：间隔(ms)、最长等待(ms) */
    private static final long POLL_INTERVAL_MS = 2000;
    private static final long POLL_TIMEOUT_MS = 120_000;

    /**
     * 脚本执行：根据 definition_id 查询元数据定义与 request_config，从 script_manage 取 script_content，
     * 将入参 params 与 request_config.userParams 中的 type 组装后调用 Aegis test-async，再轮询 run-result 直到拿到结果后返回统一格式。
     */
    public ScriptRunResponse runScript(ScriptRunRequest request) {
        MetadataDefinition definition = metadataDefinitionMapper.selectByIdWithTimestamp(request.getDefinitionId());
        if (definition == null) {
            throw new MSException("元数据不存在: " + request.getDefinitionId());
        }
        Map<String, Object> requestConfig = definition.getRequestConfig();
        if (requestConfig == null) {
            requestConfig = new HashMap<>();
        }

        String scriptContent;
        String scriptTypeLower;
        if ("SCRIPT".equals(definition.getProtocol()) && StringUtils.isNotBlank(definition.getScriptContent())) {
            ScriptManage scriptManage = scriptManageMapper.selectByIdWithTimestamp(definition.getScriptContent());
            if (scriptManage == null) {
                throw new MSException("脚本不存在: " + definition.getScriptContent());
            }
            scriptContent = scriptManage.getScriptContent();
            scriptTypeLower = scriptManage.getScriptType() == null ? "python" : scriptManage.getScriptType().toLowerCase();
        } else {
            scriptContent = definition.getScriptContent() != null ? definition.getScriptContent() : "";
            scriptTypeLower = "SCRIPT".equals(definition.getProtocol()) ? "python" : "python";
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userParamsConfig = (List<Map<String, Object>>) requestConfig.get("userParams");
        Map<String, String> paramNameToType = new HashMap<>();
        if (userParamsConfig != null) {
            for (Map<String, Object> p : userParamsConfig) {
                Object name = p.get("paramName");
                Object type = p.get("paramType");
                if (name != null && type != null) {
                    paramNameToType.put(name.toString(), type.toString());
                }
            }
        }

        String chainCallTemplate = null;
        Object chainObj = requestConfig.get("chainCallTemplate");
        if (chainObj != null) {
            chainCallTemplate = chainObj.toString();
        }

        List<Map<String, Object>> paramsList = new ArrayList<>();
        if (request.getParams() != null && !request.getParams().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                Map<String, Object> param = new HashMap<>();
                param.put("key", entry.getKey());
                param.put("value", entry.getValue() != null ? entry.getValue() : "");
                String type = paramNameToType.getOrDefault(entry.getKey(), "str");
                param.put("type", type);
                param.put("valid", true);
                paramsList.add(param);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", scriptTypeLower);
        body.put("script", scriptContent);
        if (chainCallTemplate != null) {
            body.put("chainCallTemplate", chainCallTemplate);
        }
        body.put("params", paramsList);
        body.put("projectId", definition.getProjectId());
        body.put("executionCount", request.getExecutionCount() != null ? request.getExecutionCount() : 1);
        body.put("environmentId", request.getEnvironmentId());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.exchange(AEGIS_SCRIPT_TEST_ASYNC_URL, HttpMethod.POST, entity, Object.class);
        Object asyncBody = response.getBody();
        String jobId = extractJobId(asyncBody);
        if (StringUtils.isBlank(jobId)) {
            return ScriptRunResponse.success(Collections.emptyList());
        }
        Object pollBody = pollRunResult(restTemplate, headers, jobId);
        Object data = buildScriptRunDataResultsOnly(pollBody);
        return ScriptRunResponse.success(data);
    }

    /**
     * 从轮询结果中仅提取 results 列表作为 data 返回。
     */
    @SuppressWarnings("unchecked")
    private Object buildScriptRunDataResultsOnly(Object pollBody) {
        if (pollBody != null && pollBody.toString().contains("轮询超时")) {
            return Collections.singletonList(Map.of(
                    "success", false,
                    "message", pollBody.toString(),
                    "output", "",
                    "error_output", "轮询超时",
                    "execution_index", 0));
        }
        if (!(pollBody instanceof Map)) {
            return Collections.emptyList();
        }
        Object innerData = ((Map<String, Object>) pollBody).get("data");
        if (innerData instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) innerData;
            Object result = dataMap.get("result");
            if (result instanceof Map) {
                Object results = ((Map<String, Object>) result).get("results");
                if (results instanceof List) {
                    return results;
                }
            }
            Object results = dataMap.get("results");
            if (results instanceof List) {
                return results;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 从 test-async 响应中解析 jobId（支持外层 data 或 data.data 包装）
     */
    @SuppressWarnings("unchecked")
    private String extractJobId(Object body) {
        if (body == null) {
            return null;
        }
        if (!(body instanceof Map)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) body;
        Object data = map.get("data");
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object jobId = dataMap.get("jobId");
            if (jobId != null && StringUtils.isNotBlank(jobId.toString())) {
                return jobId.toString();
            }
            Object innerData = dataMap.get("data");
            if (innerData instanceof Map) {
                Object innerJobId = ((Map<String, Object>) innerData).get("jobId");
                if (innerJobId != null && StringUtils.isNotBlank(innerJobId.toString())) {
                    return innerJobId.toString();
                }
            }
        }
        return null;
    }

    /**
     * 轮询 GET /data/code/run-result/{jobId}，直到任务结束或超时，返回最后一次响应体。
     */
    @SuppressWarnings("unchecked")
    private Object pollRunResult(RestTemplate restTemplate, HttpHeaders headers, String jobId) {
        String url = String.format(AEGIS_RUN_RESULT_URL_TEMPLATE, jobId);
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        Object lastBody = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                ResponseEntity<Object> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
                lastBody = resp.getBody();
                if (lastBody == null) {
                    break;
                }
                if (lastBody instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) lastBody;
                    Object data = map.get("data");
                    if (data instanceof Map) {
                        Object status = ((Map<String, Object>) data).get("status");
                        String statusStr = status != null ? status.toString().toUpperCase() : "";
                        if ("SUCCESS".equals(statusStr) || "COMPLETED".equals(statusStr) || "DONE".equals(statusStr)
                                || "FAILED".equals(statusStr) || "ERROR".equals(statusStr)) {
                            return lastBody;
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.warn("轮询 run-result 失败 jobId={}, error={}", jobId, e.getMessage());
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return lastBody != null ? lastBody : Map.of("message", "轮询超时，未获取到执行结果", "jobId", jobId);
    }

    /**
     * 分页查询元数据定义列表
     */
    public List<MetadataDefinition> list(MetadataDefinitionPageRequest request) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetadataDefinition::getProjectId, request.getProjectId())
                .isNull(MetadataDefinition::getDeletedTime);
        
        if (StringUtils.isNotBlank(request.getProtocol())) {
            wrapper.eq(MetadataDefinition::getProtocol, request.getProtocol());
        }
        
        if (StringUtils.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword().replace("'", "''");
            wrapper.apply("LOWER(name) LIKE LOWER(CONCAT('%', {0}, '%'))", keyword);
        }
        
        if (StringUtils.isNotBlank(request.getCreateUser())) {
            String createUser = request.getCreateUser().replace("'", "''");
            wrapper.apply("LOWER(create_user) LIKE LOWER(CONCAT('%', {0}, '%'))", createUser);
        }
        
        wrapper.orderByDesc(MetadataDefinition::getCreateTime);
        
        return metadataDefinitionMapper.selectList(wrapper);
    }

    /**
     * 复制元数据定义
     */
    public String copy(String id, String userId) {
        MetadataDefinition source = metadataDefinitionMapper.selectByIdWithTimestamp(id);
        if (source == null) {
            throw new MSException("元数据不存在");
        }

        String newId = IDGenerator.nextStr();
        long currentTime = System.currentTimeMillis();

        MetadataDefinition copy = new MetadataDefinition();
        copy.setId(newId);
        copy.setProjectId(source.getProjectId());
        copy.setModuleId(source.getModuleId());
        copy.setName(source.getName() + "_copy");
        copy.setProtocol(source.getProtocol());
        copy.setVersion(1);
        copy.setIsLatest(true);
        copy.setIsCase(source.getIsCase());
        copy.setDescription(source.getDescription());
        copy.setRequestConfig(source.getRequestConfig());
        copy.setResponseConfig(source.getResponseConfig());
        copy.setScriptContent(source.getScriptContent());
        copy.setTags(source.getTags());
        copy.setCreateUser(userId);
        copy.setCreateTime(currentTime);
        copy.setUpdateTime(currentTime);
        // deleted_time 不设置，默认为 NULL（表示未删除）
        
        metadataDefinitionMapper.insert(copy);

        return newId;
    }

    /**
     * 导入 Dubbo Swagger API（异步执行）
     */
    @Async("metadataImportExecutor")
    public void importDubboSwagger(DubboSwaggerImportRequest request, String userId) {
        try {
            io.vanguard.testops.metadata.domain.MetadataModule module = metadataModuleMapper.selectByIdWithTimestamp(request.getModuleId());
            if (module == null) {
                throw new MSException("模块不存在: " + request.getModuleId());
            }
            
            String projectId = request.getProjectId();
            if (!projectId.equals(module.getProjectId())) {
                throw new MSException("模块ID与项目ID不匹配");
            }
            
            String moduleId = request.getModuleId();
            
            String normalizedUrl = normalizeAndValidateUrl(request.getUrl());
            String jsonContent = downloadSwaggerJson(normalizedUrl);
            
            Map<String, Object> jsonMap = JSON.parseObject(jsonContent, Map.class);
            if (jsonMap == null) {
                throw new MSException("无法解析 JSON 响应");
            }
            
            Object codeObj = jsonMap.get("code");
            if (codeObj != null) {
                Integer code = codeObj instanceof Integer ? (Integer) codeObj 
                        : codeObj instanceof Number ? ((Number) codeObj).intValue() : null;
                if (code != null && code != 200) {
                    Object messageObj = jsonMap.get("message");
                    String errorMsg = messageObj != null ? messageObj.toString() : "请求失败";
                    throw new MSException("Dubbo Swagger 请求失败: " + errorMsg + " (code: " + code + ")");
                }
            }
            
            Object dataObj = jsonMap.get("data");
            if (dataObj == null || !(dataObj instanceof Map)) {
                throw new MSException("响应中缺少 data 字段");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object servicesObj = data.get("x-dubbo-services");
            if (servicesObj == null || !(servicesObj instanceof List)) {
                throw new MSException("响应中缺少 x-dubbo-services 字段");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> services = (List<Map<String, Object>>) servicesObj;
            
            // 获取 x-dubbo-definitions
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> definitions = (Map<String, Map<String, Object>>) data.get("x-dubbo-definitions");
            if (definitions == null) {
                definitions = new HashMap<>();
            }
            
            Map<String, MetadataDefinition> existingInterfacesMap = metadataDefinitionMapper.selectByProjectId(projectId).stream()
                    .filter(api -> "DUBBO".equals(api.getProtocol()) && api.getRequestConfig() != null)
                    .filter(api -> api.getIsCase() == null || Boolean.FALSE.equals(api.getIsCase()))
                    .map(api -> {
                        Object interfaceNameObj = api.getRequestConfig().get("interfaceName");
                        Object methodNameObj = api.getRequestConfig().get("methodName");
                        if (interfaceNameObj != null && methodNameObj != null) {
                            String key = interfaceNameObj.toString() + "#" + methodNameObj.toString();
                            return new AbstractMap.SimpleEntry<>(key, api);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
            
            List<MetadataDefinition> importList = new ArrayList<>();
            List<MetadataDefinition> updateList = new ArrayList<>();
            List<String> existInfo = new ArrayList<>();
            
            // 缓存已查找的模块，避免重复查询
            Map<String, String> applicationModuleMap = new HashMap<>();
            Map<String, String> interfaceModuleMap = new HashMap<>();
            
            for (Map<String, Object> service : services) {
                String application = (String) service.get("application");
                if (StringUtils.isBlank(application)) {
                    continue;
                }
                
                // 创建应用父节点（application）
                String applicationModuleId = applicationModuleMap.get(application);
                if (applicationModuleId == null) {
                    applicationModuleId = findOrCreateModule(projectId, moduleId, application, userId, "DUBBO");
                    applicationModuleMap.put(application, applicationModuleId);
                }
                
                Object interfacesObj = service.get("interfaces");
                if (interfacesObj == null || !(interfacesObj instanceof List)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> interfaces = (List<Map<String, Object>>) interfacesObj;
                
                for (Map<String, Object> interfaceItem : interfaces) {
                    String interfaceName = (String) interfaceItem.get("interfaceName");
                    if (StringUtils.isBlank(interfaceName)) {
                        continue;
                    }
                    
                    if (interfaceName.endsWith("Endpoint")) {
                        continue;
                    }
                    
                    // 从 interfaceName 中提取最后一个 . 后的部分作为模块名
                    String moduleName = extractModuleNameFromInterface(interfaceName);
                    
                    // 查找或创建接口维度节点（在应用父节点下）
                    String interfaceKey = application + "#" + interfaceName;
                    String interfaceModuleId = interfaceModuleMap.get(interfaceKey);
                    if (interfaceModuleId == null) {
                        interfaceModuleId = findOrCreateModule(projectId, applicationModuleId, moduleName, userId, "DUBBO");
                        interfaceModuleMap.put(interfaceKey, interfaceModuleId);
                    }
                    
                    Object methodsObj = interfaceItem.get("methods");
                    if (methodsObj == null || !(methodsObj instanceof List)) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> methods = (List<Map<String, Object>>) methodsObj;
                    
                    for (Map<String, Object> method : methods) {
                        String methodName = (String) method.get("methodName");
                        if (StringUtils.isBlank(methodName)) {
                            continue;
                        }
                        
                        String interfaceMethodKey = interfaceName + "#" + methodName;
                        MetadataDefinition existing = existingInterfacesMap.get(interfaceMethodKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromDubboJson(
                                    interfaceName, methodName, method, application, 
                                    projectId, interfaceModuleId, userId, definitions);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(interfaceMethodKey);
                        } else {
                        MetadataDefinition definition = createDefinitionFromDubboJson(
                                interfaceName, methodName, method, application, 
                                projectId, interfaceModuleId, userId, definitions);
                        importList.add(definition);
                        }
                    }
                }
            }
            
            importList.forEach(metadataDefinitionMapper::insert);
            updateList.forEach(metadataDefinitionMapper::updateById);
            
            LogUtils.info("异步导入 Dubbo Swagger 完成: 已存在 {} 个接口(已更新), 导入 {} 个接口", existInfo.size(), importList.size());
            
        } catch (MSException e) {
            LogUtils.error("异步导入 Dubbo Swagger 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            LogUtils.error("异步导入 Dubbo Swagger 失败", e);
        }
    }

    /**
     * 导入 Swagger API（异步执行）
     */
    @Async("metadataImportExecutor")
    public void importSwagger(SwaggerImportRequest request, String userId) {
        try {
            String normalizedUrl = normalizeAndValidateUrl(request.getUrl());
            
            // 从 URL 中提取网关路径
            String gatewayPath = extractGatewayPathFromUrl(normalizedUrl);
            
            // 先下载 JSON 内容，避免 Swagger 解析器的 URL 处理问题
            String jsonContent = downloadSwaggerJson(normalizedUrl);
            SwaggerParseResult result = new OpenAPIParser().readContents(jsonContent, null, null);
            
            if (result == null || result.getOpenAPI() == null) {
                String errorMsg = result != null && result.getMessages() != null && !result.getMessages().isEmpty()
                        ? String.join(", ", result.getMessages()) : "无法解析 Swagger 内容";
                throw new MSException("Swagger 解析失败: " + errorMsg);
            }
            
            OpenAPI openAPI = result.getOpenAPI();
            String openApiVersion = openAPI.getOpenapi();
            if (openApiVersion == null || (!openApiVersion.startsWith("3.0") && !openApiVersion.startsWith("3.1"))) {
                throw new MSException("不支持的 OpenAPI 版本: " + openApiVersion);
            }
            
            String parentModuleId = StringUtils.isNotBlank(request.getModuleId()) 
                    ? request.getModuleId() : "ROOT";
            
            Map<String, MetadataDefinition> existingPathsMap = metadataDefinitionMapper.selectByProjectId(request.getProjectId()).stream()
                    .filter(api -> api.getRequestConfig() != null)
                    .filter(api -> "HTTP".equals(api.getProtocol()))
                    .filter(api -> api.getIsCase() == null || Boolean.FALSE.equals(api.getIsCase()))
                    .map(api -> {
                        Object pathObj = api.getRequestConfig().get("path");
                        Object methodObj = api.getRequestConfig().get("method");
                        if (pathObj != null && methodObj != null) {
                            String key = methodObj.toString() + " " + pathObj.toString();
                            return new AbstractMap.SimpleEntry<>(key, api);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
            
            List<MetadataDefinition> importList = new ArrayList<>();
            List<MetadataDefinition> updateList = new ArrayList<>();
            List<String> existInfo = new ArrayList<>();
            
            // 缓存已查找的模块，避免重复查询
            Map<String, String> pathModuleMap = new HashMap<>();
            
            // 创建应用父节点（serviceCode）
            String applicationModuleId = findOrCreateModule(request.getProjectId(), parentModuleId, request.getServiceCode(), userId, "API");
            
            if (openAPI.getPaths() != null) {
                for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                    String path = entry.getKey();
                    PathItem pathItem = entry.getValue();
                    
                    // 拼接网关路径
                    String finalPath = path;
                    if (StringUtils.isNotBlank(gatewayPath)) {
                        String normalizedGatewayPath = gatewayPath.startsWith("/") ? gatewayPath : "/" + gatewayPath;
                        String normalizedPath = path.startsWith("/") ? path : "/" + path;
                        finalPath = normalizedGatewayPath + normalizedPath;
                    }
                    
                    // 从路径中提取第一个 / 后的第一部分作为模块名（使用原始路径，因为模块名应该基于业务路径）
                    String moduleName = extractModuleNameFromPath(path);
                    
                    // 查找或创建接口维度节点（在应用父节点下）
                    String pathModuleId = pathModuleMap.get(moduleName);
                    if (pathModuleId == null) {
                        pathModuleId = findOrCreateModule(request.getProjectId(), applicationModuleId, moduleName, userId, "API");
                        pathModuleMap.put(moduleName, pathModuleId);
                    }
                    
                    if (pathItem.getGet() != null) {
                        String pathKey = "GET " + finalPath;
                        MetadataDefinition existing = existingPathsMap.get(pathKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromSwagger(finalPath, "GET", pathItem.getGet(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(pathKey);
                        } else {
                            importList.add(createDefinitionFromSwagger(finalPath, "GET", pathItem.getGet(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI));
                        }
                    }
                    if (pathItem.getPost() != null) {
                        String pathKey = "POST " + finalPath;
                        MetadataDefinition existing = existingPathsMap.get(pathKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromSwagger(finalPath, "POST", pathItem.getPost(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(pathKey);
                        } else {
                            importList.add(createDefinitionFromSwagger(finalPath, "POST", pathItem.getPost(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI));
                        }
                    }
                    if (pathItem.getPut() != null) {
                        String pathKey = "PUT " + finalPath;
                        MetadataDefinition existing = existingPathsMap.get(pathKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromSwagger(finalPath, "PUT", pathItem.getPut(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(pathKey);
                        } else {
                            importList.add(createDefinitionFromSwagger(finalPath, "PUT", pathItem.getPut(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI));
                        }
                    }
                    if (pathItem.getDelete() != null) {
                        String pathKey = "DELETE " + finalPath;
                        MetadataDefinition existing = existingPathsMap.get(pathKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromSwagger(finalPath, "DELETE", pathItem.getDelete(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(pathKey);
                        } else {
                            importList.add(createDefinitionFromSwagger(finalPath, "DELETE", pathItem.getDelete(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI));
                        }
                    }
                    if (pathItem.getPatch() != null) {
                        String pathKey = "PATCH " + finalPath;
                        MetadataDefinition existing = existingPathsMap.get(pathKey);
                        if (existing != null) {
                            MetadataDefinition newDefinition = createDefinitionFromSwagger(finalPath, "PATCH", pathItem.getPatch(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI);
                            updateExistingDefinition(existing, newDefinition);
                            updateList.add(existing);
                            existInfo.add(pathKey);
                        } else {
                            importList.add(createDefinitionFromSwagger(finalPath, "PATCH", pathItem.getPatch(),
                                    request.getProjectId(), pathModuleId, request.getServiceCode(), userId, openAPI));
                        }
                    }
                }
            }
            
            importList.forEach(metadataDefinitionMapper::insert);
            updateList.forEach(metadataDefinitionMapper::updateById);
            
            LogUtils.info("异步导入 Swagger 完成: 已存在 {} 个接口(已更新), 导入 {} 个接口", existInfo.size(), importList.size());
            
        } catch (MSException e) {
            LogUtils.error("异步导入 Swagger 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            LogUtils.error("异步导入 Swagger 失败", e);
        }
    }
    
    /**
     * 规范化并验证 Swagger URL
     */
    private String normalizeAndValidateUrl(String swaggerUrl) {
        if (StringUtils.isBlank(swaggerUrl)) {
            throw new MSException("Swagger URL 不能为空");
        }
        
        swaggerUrl = swaggerUrl.trim().replaceAll("(?<!:)//+", "/");
        if (swaggerUrl.endsWith("/") && swaggerUrl.length() > 1) {
            swaggerUrl = swaggerUrl.substring(0, swaggerUrl.length() - 1);
        }
        
        try {
            URI uri = new URI(swaggerUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new MSException("Swagger URL 必须使用 HTTP 或 HTTPS 协议");
            }
        } catch (Exception e) {
            throw new MSException("URL 格式错误: " + e.getMessage());
        }
        
        return swaggerUrl;
    }
    
    /**
     * 下载 Swagger JSON 内容
     */
    private String downloadSwaggerJson(String swaggerUrl) {
        try {
            URI uri = new URI(swaggerUrl);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json, */*");
            
            // 添加 Referer 头（很多服务器需要）
            String referer = swaggerUrl;
            if (swaggerUrl.contains("/openapi.json")) {
                referer = swaggerUrl.replace("/openapi.json", "/docs");
            } else if (swaggerUrl.contains("/v3/api-docs")) {
                referer = swaggerUrl.replace("/v3/api-docs", "/docs");
            }
            conn.setRequestProperty("Referer", referer);
            
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                String errorMsg = "无法访问 Swagger URL，HTTP 状态码: " + responseCode;
                try (java.io.InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorBody = new String(errorStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        if (errorBody.contains("Ambiguous URI")) {
                            errorMsg += "。服务器返回 'Ambiguous URI empty segment' 错误，请检查 URL 路径是否正确";
                        } else if (errorBody.length() < 500) {
                            errorMsg += "。错误: " + errorBody;
                        }
                    }
                }
                conn.disconnect();
                throw new MSException(errorMsg);
            }
            
            try (java.io.InputStream inputStream = conn.getInputStream()) {
                String jsonContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                conn.disconnect();
                return jsonContent;
            }
        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            throw new MSException("下载 Swagger JSON 失败: " + e.getMessage());
        }
    }
    
    /**
     * 从 Swagger 创建元数据定义
     */
    private MetadataDefinition createDefinitionFromSwagger(String path, String method, 
                                                           io.swagger.v3.oas.models.Operation operation,
                                                           String projectId, String moduleId, 
                                                           String serviceCode, String userId, OpenAPI openAPI) {
        long currentTime = System.currentTimeMillis();
        
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("path", path);
        requestConfig.put("method", method);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        requestConfig.put("headers", headers);
        
        Map<String, Object> params = new HashMap<>();
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(param -> {
                Map<String, Object> info = new HashMap<>();
                info.put("type", param.getSchema() != null ? param.getSchema().getType() : "string");
                info.put("in", param.getIn());
                info.put("required", param.getRequired() != null && param.getRequired());
                params.put(param.getName(), info);
            });
        }
        requestConfig.put("params", params);
        
        // 解析 requestBody 的 schema，只存储 properties（仅当有 $ref 或 properties 时）
        Map<String, Object> requestBody = new HashMap<>();
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            operation.getRequestBody().getContent().forEach((contentType, mediaType) -> {
                if (mediaType.getSchema() != null) {
                    Map<String, Object> properties = extractSchemaProperties(mediaType.getSchema(), openAPI);
                    if (properties != null && !properties.isEmpty()) {
                        requestBody.putAll(properties);
                    }
                }
            });
        }
        // body 直接存储 properties 内容，如果为空则存空对象
        requestConfig.put("body", requestBody);
        
        Map<String, Object> responseConfig = new HashMap<>();
        
        // 解析 responses 的 schema，只存储 properties（仅当有 $ref 或 properties 时）
        Map<String, Object> responseBody = new HashMap<>();
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            operation.getResponses().forEach((statusCodeKey, apiResponse) -> {
                if (apiResponse.getContent() != null && !apiResponse.getContent().isEmpty()) {
                    apiResponse.getContent().forEach((contentType, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            Map<String, Object> properties = extractSchemaProperties(mediaType.getSchema(), openAPI);
                            if (properties != null && !properties.isEmpty()) {
                                responseBody.putAll(properties);
                            }
                        }
                    });
                }
            });
        }
        // body 直接存储 properties 内容，如果为空则存空对象
        responseConfig.put("body", responseBody);
        
        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(IDGenerator.nextStr());
        definition.setProjectId(projectId);
        definition.setModuleId(moduleId);
        // 如果 description 不为空，使用 description 作为接口名称；否则使用路径的最后一个 / 后的部分
        String name;
        if (StringUtils.isNotBlank(operation.getDescription())) {
            name = operation.getDescription();
        } else {
            name = extractNameFromPath(path);
        }
        definition.setName(name);
        definition.setProtocol("HTTP");
        definition.setVersion(1);
        definition.setIsLatest(true);
        definition.setDescription(operation.getDescription() != null ? operation.getDescription() : "Swagger 导入");
        definition.setRequestConfig(requestConfig);
        definition.setResponseConfig(responseConfig);
        definition.setCreateUser(userId);
        definition.setCreateTime(currentTime);
        definition.setUpdateTime(currentTime);
        definition.setTags(operation.getTags() != null && !operation.getTags().isEmpty() 
                ? operation.getTags() : Collections.singletonList(serviceCode));
        
        return definition;
    }

    /**
     * 更新已存在的元数据定义
     * @param existing 已存在的定义
     * @param newDefinition 新创建的定义（包含最新数据）
     */
    private void updateExistingDefinition(MetadataDefinition existing, MetadataDefinition newDefinition) {
        long currentTime = System.currentTimeMillis();
        
        existing.setName(newDefinition.getName());
        existing.setDescription(newDefinition.getDescription());
        existing.setRequestConfig(newDefinition.getRequestConfig());
        existing.setResponseConfig(newDefinition.getResponseConfig());
        existing.setTags(newDefinition.getTags());
        existing.setModuleId(newDefinition.getModuleId());
        existing.setUpdateTime(currentTime);
    }
    
    /**
     * 提取 schema 的 properties，返回可直接调用的示例值格式
     * 如果是基本类型（只有 type），返回 null
     */
    private Map<String, Object> extractSchemaProperties(Schema<?> schema, OpenAPI openAPI) {
        return extractSchemaProperties(schema, openAPI, new HashSet<>(), 0);
    }
    
    /**
     * 提取 schema 的 properties，返回可直接调用的示例值格式（带递归控制）
     */
    private Map<String, Object> extractSchemaProperties(Schema<?> schema, OpenAPI openAPI, Set<String> visited, int depth) {
        if (schema == null) {
            return null;
        }
        
        if (depth > 10) {
            return null;
        }
        
        String ref = schema.get$ref();
        if (ref != null) {
            if (visited.contains(ref)) {
                return null;
            }
            visited.add(ref);
            try {
                Schema<?> resolvedSchema = resolveSchemaRef(ref, openAPI);
            if (resolvedSchema != null) {
                    return extractSchemaProperties(resolvedSchema, openAPI, visited, depth + 1);
                }
            } finally {
                visited.remove(ref);
            }
            return null;
        }
        
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new HashMap<>();
            schema.getProperties().forEach((propName, propSchema) -> {
                Object exampleValue = generateExampleValue(propSchema, openAPI, visited, depth + 1);
                if (exampleValue != null) {
                    properties.put(propName, exampleValue);
                }
            });
            return properties;
        }
        
        return null;
    }
    
    /**
     * 根据 schema 生成示例值
     */
    private Object generateExampleValue(Schema<?> schema, OpenAPI openAPI) {
        return generateExampleValue(schema, openAPI, new HashSet<>(), 0);
    }
    
    /**
     * 根据 schema 生成示例值（带递归控制）
     */
    private Object generateExampleValue(Schema<?> schema, OpenAPI openAPI, Set<String> visited, int depth) {
        if (schema == null) {
            return null;
        }
        
        if (depth > 10) {
            return null;
        }
        
        String ref = schema.get$ref();
        if (ref != null) {
            if (visited.contains(ref)) {
                return null;
            }
            visited.add(ref);
            try {
                Schema<?> resolvedSchema = resolveSchemaRef(ref, openAPI);
                if (resolvedSchema != null) {
                    return generateExampleValue(resolvedSchema, openAPI, visited, depth + 1);
                }
            } finally {
                visited.remove(ref);
            }
            return null;
        }
        
        if (schema.getExample() != null) {
            return schema.getExample();
        }
        
        if (schema.getDefault() != null) {
            return schema.getDefault();
        }
        
        String type = schema.getType();
        if (type == null) {
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                Map<String, Object> obj = new HashMap<>();
                schema.getProperties().forEach((propName, propSchema) -> {
                    Object value = generateExampleValue(propSchema, openAPI, visited, depth + 1);
                    if (value != null) {
                        obj.put(propName, value);
                    }
                });
                return obj;
            }
            return null;
        }
        
        switch (type.toLowerCase()) {
            case "string":
                return "string";
            case "integer":
            case "number":
                String format = schema.getFormat();
                if ("int64".equals(format) || "long".equals(format)) {
                    return 0L;
                } else if ("float".equals(format)) {
                    return 0.0f;
                } else if ("double".equals(format)) {
                    return 0.0d;
                }
                return 0;
            case "boolean":
                return false;
            case "array":
                if (schema.getItems() != null) {
                    Object itemValue = generateExampleValue(schema.getItems(), openAPI, visited, depth + 1);
                    if (itemValue != null) {
                        return Collections.singletonList(itemValue);
                    }
                }
                return Collections.emptyList();
            case "object":
                if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                    Map<String, Object> obj = new HashMap<>();
                    schema.getProperties().forEach((propName, propSchema) -> {
                        Object value = generateExampleValue(propSchema, openAPI, visited, depth + 1);
                        if (value != null) {
                            obj.put(propName, value);
                        }
                    });
                    return obj;
                }
                return new HashMap<>();
            default:
                return null;
        }
    }
    
    /**
     * 解析 $ref 引用，从 components/schemas 中获取实际的 schema
     */
    private Schema<?> resolveSchemaRef(String ref, OpenAPI openAPI) {
        if (ref == null || openAPI == null || openAPI.getComponents() == null 
                || openAPI.getComponents().getSchemas() == null) {
            return null;
        }
        
        // $ref 格式: "#/components/schemas/OrderAuditFulfillAuditSingleParam"
        if (ref.startsWith("#/components/schemas/")) {
            String schemaName = ref.substring("#/components/schemas/".length());
            return openAPI.getComponents().getSchemas().get(schemaName);
        }
        
        return null;
    }
    
    /**
     * 将 Swagger Schema 转换为 Map
     */
    private Map<String, Object> parseSchemaToMap(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        
        Map<String, Object> schemaMap = new HashMap<>();
        
        if (schema.getType() != null) {
            schemaMap.put("type", schema.getType());
        }
        if (schema.getFormat() != null) {
            schemaMap.put("format", schema.getFormat());
        }
        if (schema.getDescription() != null) {
            schemaMap.put("description", schema.getDescription());
        }
        if (schema.getExample() != null) {
            schemaMap.put("example", schema.getExample());
        }
        if (schema.getDefault() != null) {
            schemaMap.put("default", schema.getDefault());
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            schemaMap.put("enum", schema.getEnum());
        }
        if (schema.getMinLength() != null) {
            schemaMap.put("minLength", schema.getMinLength());
        }
        if (schema.getMaxLength() != null) {
            schemaMap.put("maxLength", schema.getMaxLength());
        }
        if (schema.getMinimum() != null) {
            schemaMap.put("minimum", schema.getMinimum());
        }
        if (schema.getMaximum() != null) {
            schemaMap.put("maximum", schema.getMaximum());
        }
        if (schema.getPattern() != null) {
            schemaMap.put("pattern", schema.getPattern());
        }
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            schemaMap.put("required", schema.getRequired());
        }
        if (schema.getItems() != null) {
            schemaMap.put("items", parseSchemaToMap(schema.getItems()));
        }
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new HashMap<>();
            schema.getProperties().forEach((propName, propSchema) -> {
                properties.put(propName, parseSchemaToMap(propSchema));
            });
            schemaMap.put("properties", properties);
        }
        if (schema.getAdditionalProperties() != null) {
            if (schema.getAdditionalProperties() instanceof Schema) {
                schemaMap.put("additionalProperties", parseSchemaToMap((Schema<?>) schema.getAdditionalProperties()));
            } else {
                schemaMap.put("additionalProperties", schema.getAdditionalProperties());
            }
        }
        if (schema.get$ref() != null) {
            schemaMap.put("$ref", schema.get$ref());
        }
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            List<Map<String, Object>> allOf = new ArrayList<>();
            schema.getAllOf().forEach(allOfSchema -> {
                allOf.add(parseSchemaToMap(allOfSchema));
            });
            schemaMap.put("allOf", allOf);
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            List<Map<String, Object>> oneOf = new ArrayList<>();
            schema.getOneOf().forEach(oneOfSchema -> {
                oneOf.add(parseSchemaToMap(oneOfSchema));
            });
            schemaMap.put("oneOf", oneOf);
        }
        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            List<Map<String, Object>> anyOf = new ArrayList<>();
            schema.getAnyOf().forEach(anyOfSchema -> {
                anyOf.add(parseSchemaToMap(anyOfSchema));
            });
            schemaMap.put("anyOf", anyOf);
        }
        
        return schemaMap;
    }
    
    /**
     * 从 Swagger Operation 中提取 Dubbo 接口键（用于去重）
     */
    private String extractDubboInterfaceKey(io.swagger.v3.oas.models.Operation operation, String path) {
        Map<String, Object> extensions = operation.getExtensions();
        if (extensions != null) {
            Object interfaceNameObj = extensions.get("x-dubbo-interface");
            Object methodNameObj = extensions.get("x-dubbo-method");
            if (interfaceNameObj != null && methodNameObj != null) {
                return interfaceNameObj.toString() + "#" + methodNameObj.toString();
            }
        }
        
        String operationId = operation.getOperationId();
        if (StringUtils.isNotBlank(operationId)) {
            if (operationId.contains("#")) {
                return operationId;
            } else if (operationId.contains(".")) {
                String[] parts = operationId.split("\\.");
                if (parts.length >= 2) {
                    String methodName = parts[parts.length - 1];
                    String interfaceName = String.join(".", Arrays.copyOf(parts, parts.length - 1));
                    return interfaceName + "#" + methodName;
                }
            }
        }
        
        String summary = operation.getSummary();
        if (StringUtils.isNotBlank(summary) && summary.contains("#")) {
            return summary;
        }
        
        if (StringUtils.isNotBlank(path)) {
            String cleanPath = path.replace("/", ".").replaceAll("^\\.", "");
            if (StringUtils.isNotBlank(cleanPath)) {
                String methodName = operationId != null ? operationId : summary != null ? summary : "invoke";
                return cleanPath + "#" + methodName;
            }
        }
        
        return null;
    }
    
    /**
     * 从 Dubbo Swagger 创建元数据定义
     */
    private MetadataDefinition createDefinitionFromDubboSwagger(io.swagger.v3.oas.models.Operation operation, 
                                                               String path,
                                                               String projectId, String moduleId, 
                                                               String serviceCode, String applicationName,
                                                               String siteTenant, String userId) {
        long currentTime = System.currentTimeMillis();
        
        Map<String, Object> requestConfig = new HashMap<>();
        
        String interfaceName = null;
        String methodName = null;
        List<String> parameterTypes = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        Map<String, Object> extensions = operation.getExtensions();
        if (extensions != null) {
            Object interfaceNameObj = extensions.get("x-dubbo-interface");
            Object methodNameObj = extensions.get("x-dubbo-method");
            if (interfaceNameObj != null) {
                interfaceName = interfaceNameObj.toString();
            }
            if (methodNameObj != null) {
                methodName = methodNameObj.toString();
            }
        }
        
        if (StringUtils.isBlank(interfaceName)) {
            String operationId = operation.getOperationId();
            if (StringUtils.isNotBlank(operationId)) {
                if (operationId.contains("#")) {
                    String[] parts = operationId.split("#", 2);
                    interfaceName = parts[0];
                    if (parts.length > 1) {
                        methodName = parts[1];
                    }
                } else if (operationId.contains(".")) {
                    String[] parts = operationId.split("\\.");
                    if (parts.length >= 2) {
                        methodName = parts[parts.length - 1];
                        interfaceName = String.join(".", Arrays.copyOf(parts, parts.length - 1));
                    } else {
                        interfaceName = operationId;
                    }
                } else {
                    interfaceName = operationId;
                }
            }
        }
        
        if (StringUtils.isBlank(interfaceName)) {
            String cleanPath = path.replace("/", ".").replaceAll("^\\.", "");
            if (StringUtils.isNotBlank(cleanPath)) {
                interfaceName = cleanPath;
            } else {
                interfaceName = serviceCode != null ? serviceCode : "com.example.service";
            }
        }
        
        if (StringUtils.isBlank(methodName)) {
            methodName = operation.getSummary() != null ? operation.getSummary() 
                    : operation.getOperationId() != null ? operation.getOperationId() 
                    : "invoke";
        }
        
        requestConfig.put("interfaceName", interfaceName);
        requestConfig.put("methodName", methodName);
        
        if (operation.getParameters() != null && !operation.getParameters().isEmpty()) {
            for (Parameter param : operation.getParameters()) {
                if (param.getSchema() != null) {
                    String paramType = param.getSchema().getType();
                    if (StringUtils.isBlank(paramType) && param.getSchema().get$ref() != null) {
                        String ref = param.getSchema().get$ref();
                        if (ref.contains("/")) {
                            paramType = ref.substring(ref.lastIndexOf("/") + 1);
                        } else {
                            paramType = ref;
                        }
                    }
                    if (StringUtils.isNotBlank(paramType)) {
                        parameterTypes.add(paramType);
                    }
                    
                    Map<String, Object> paramValue = new HashMap<>();
                    if (param.getSchema().getExample() != null) {
                        paramValue.put(param.getName(), param.getSchema().getExample());
                    } else {
                        paramValue.put(param.getName(), null);
                    }
                    params.add(paramValue);
                }
            }
        }
        
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            operation.getRequestBody().getContent().forEach((contentType, mediaType) -> {
                if (mediaType.getSchema() != null) {
                    String paramType = mediaType.getSchema().getType();
                    if (StringUtils.isBlank(paramType) && mediaType.getSchema().get$ref() != null) {
                        String ref = mediaType.getSchema().get$ref();
                        if (ref.contains("/")) {
                            paramType = ref.substring(ref.lastIndexOf("/") + 1);
                        } else {
                            paramType = ref;
                        }
                    }
                    if (StringUtils.isNotBlank(paramType)) {
                        parameterTypes.add(paramType);
                    }
                    
                    Object example = mediaType.getExample();
                    if (example == null && mediaType.getSchema().getExample() != null) {
                        example = mediaType.getSchema().getExample();
                    }
                    if (example != null) {
                        params.add(example);
                    } else {
                        params.add(new HashMap<>());
                    }
                }
            });
        }
        
        requestConfig.put("parameterTypes", parameterTypes);
        requestConfig.put("params", params);
        
        if (StringUtils.isNotBlank(applicationName)) {
            requestConfig.put("applicationName", applicationName);
        }
        if (StringUtils.isNotBlank(siteTenant)) {
            requestConfig.put("siteTenant", siteTenant);
        }
        
        Map<String, Object> responseConfig = new HashMap<>();
        List<Map<String, Object>> responses = new ArrayList<>();
        
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            operation.getResponses().forEach((statusCodeKey, apiResponse) -> {
                Map<String, Object> responseItem = new HashMap<>();
                
                String statusCode = StringUtils.equals("default", statusCodeKey) ? "200" : statusCodeKey;
                responseItem.put("statusCode", statusCode);
                
                if (apiResponse.getDescription() != null) {
                    responseItem.put("description", apiResponse.getDescription());
                }
                
                if (apiResponse.getContent() != null && !apiResponse.getContent().isEmpty()) {
                    List<Map<String, Object>> contentList = new ArrayList<>();
                    apiResponse.getContent().forEach((contentType, mediaType) -> {
                        Map<String, Object> contentItem = new HashMap<>();
                        contentItem.put("contentType", contentType);
                        
                        if (mediaType.getSchema() != null) {
                            contentItem.put("schema", parseSchemaToMap(mediaType.getSchema()));
                        }
                        if (mediaType.getExample() != null) {
                            contentItem.put("example", mediaType.getExample());
                        }
                        
                        contentList.add(contentItem);
                    });
                    responseItem.put("content", contentList);
                }
                
                responses.add(responseItem);
            });
        }
        
        if (!responses.isEmpty()) {
            responseConfig.put("responses", responses);
        }
        
        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(IDGenerator.nextStr());
        definition.setProjectId(projectId);
        definition.setModuleId(moduleId);
        definition.setName(operation.getSummary() != null ? operation.getSummary() : methodName);
        definition.setProtocol("DUBBO");
        definition.setVersion(1);
        definition.setIsLatest(true);
        definition.setDescription(operation.getDescription() != null ? operation.getDescription() : "Dubbo Swagger 导入");
        definition.setRequestConfig(requestConfig);
        definition.setResponseConfig(responseConfig);
        definition.setCreateUser(userId);
        definition.setCreateTime(currentTime);
        definition.setUpdateTime(currentTime);
        definition.setTags(operation.getTags() != null && !operation.getTags().isEmpty() 
                ? operation.getTags() : Collections.singletonList(serviceCode));
        
        return definition;
    }

    /**
     * 从 Dubbo JSON 数据创建元数据定义
     */
    private MetadataDefinition createDefinitionFromDubboJson(String interfaceName, String methodName,
                                                             Map<String, Object> methodData,
                                                             String applicationName,
                                                             String projectId, String moduleId, String userId,
                                                             Map<String, Map<String, Object>> definitions) {
        long currentTime = System.currentTimeMillis();
        
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("interfaceName", interfaceName);
        requestConfig.put("methodName", methodName);
        
        List<String> parameterTypes = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        Object parametersObj = methodData.get("parameters");
        if (parametersObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) parametersObj;
            for (Map<String, Object> param : parameters) {
                // 无论 schema 是否为 null，只要 type 存在就添加到 parameterTypes
                Object typeObj = param.get("type");
                String paramType = null;
                if (typeObj != null) {
                    paramType = typeObj.toString();
                    if (StringUtils.isNotBlank(paramType)) {
                        parameterTypes.add(paramType);
                    }
                }
                
                // 从 x-dubbo-definitions 中解析参数定义
                Object schemaObj = param.get("schema");
                Object paramValue = null;
                
                // 优先处理 schema 引用
                if (schemaObj != null && schemaObj instanceof String) {
                    String schemaRef = (String) schemaObj;
                    // schemaRef 格式: "#/x-dubbo-definitions/com.spotter.order.web.api.request.config.FulfillConfigQueryReq"
                    if (schemaRef.startsWith("#/x-dubbo-definitions/")) {
                        String definitionKey = schemaRef.substring("#/x-dubbo-definitions/".length());
                        Map<String, Object> paramDefinition = definitions.get(definitionKey);
                        if (paramDefinition != null) {
                            // 解析 properties 并生成示例值格式
                            paramValue = generateDubboExampleValue(paramDefinition, definitions);
                        }
                    }
                }
                
                // 如果 schema 为 null，但 type 包含泛型参数，尝试从泛型参数中提取类型并解析
                if (paramValue == null && paramType != null && schemaObj == null) {
                    paramValue = generateExampleFromGenericType(paramType, definitions);
                }
                
                // 如果没有解析到值，根据类型生成基本类型的示例值
                if (paramValue == null && paramType != null) {
                    paramValue = generateBasicTypeExample(paramType);
                }
                
                // 如果还是没有值，使用空对象
                if (paramValue == null) {
                    paramValue = new HashMap<>();
                }
                
                params.add(paramValue);
            }
        }
        
        requestConfig.put("parameterTypes", parameterTypes);
        requestConfig.put("params", params);
        
        if (StringUtils.isNotBlank(applicationName)) {
            requestConfig.put("applicationName", applicationName);
        }
        
        Map<String, Object> responseConfig = new HashMap<>();
        List<String> responseParameterTypes = new ArrayList<>();
        Object responseBody = null;
        
        Object returnTypeObj = methodData.get("returnType");
        Object returnSchemaObj = methodData.get("returnSchema");
        
        if (returnTypeObj != null) {
            String returnType = returnTypeObj.toString();
            responseParameterTypes.add(returnType);
            
            // 解析返回类型的 schema
            if (returnSchemaObj != null && returnSchemaObj instanceof String) {
                String schemaRef = (String) returnSchemaObj;
                if (schemaRef.startsWith("#/x-dubbo-definitions/")) {
                    String definitionKey = schemaRef.substring("#/x-dubbo-definitions/".length());
                    Map<String, Object> returnDefinition = definitions.get(definitionKey);
                    if (returnDefinition != null) {
                        // 生成示例值
                        responseBody = generateDubboExampleValue(returnDefinition, definitions);
                    }
                }
            } else if (returnSchemaObj == null && returnType != null) {
                // 如果 returnSchema 为 null，但 returnType 是泛型类型，尝试从泛型参数中提取类型并解析
                if (returnType.contains("<") && returnType.contains(">")) {
                    // 泛型类型，使用 generateExampleFromGenericType 生成示例值
                    responseBody = generateExampleFromGenericType(returnType, definitions);
                } else if (isBasicType(returnType)) {
                    // 基本类型，生成基本类型的示例值
                    responseBody = generateBasicTypeExample(returnType);
                } else {
                    // 普通类型，尝试从 definitions 中查找
                    Map<String, Object> returnDefinition = definitions.get(returnType);
                    if (returnDefinition != null) {
                        responseBody = generateDubboExampleValue(returnDefinition, definitions);
                    }
                }
            }
        }
        
        responseConfig.put("parameterTypes", responseParameterTypes);
        // body 存储生成的示例值，如果为空则存空对象
        responseConfig.put("body", responseBody != null ? responseBody : new HashMap<>());
        
        MetadataDefinition definition = new MetadataDefinition();
        definition.setId(IDGenerator.nextStr());
        definition.setProjectId(projectId);
        definition.setModuleId(moduleId);
        // name 统一存 methodName
        definition.setName(methodName);
        definition.setProtocol("DUBBO");
        definition.setVersion(1);
        definition.setIsLatest(true);
        definition.setDescription("Dubbo 接口: " + interfaceName + " 方法: " + methodName);
        definition.setRequestConfig(requestConfig);
        definition.setResponseConfig(responseConfig);
        definition.setCreateUser(userId);
        definition.setCreateTime(currentTime);
        definition.setUpdateTime(currentTime);
        definition.setTags(Collections.singletonList(applicationName != null ? applicationName : "dubbo"));
        
        return definition;
    }
    
    /**
     * 从 Dubbo definition 生成示例值（可直接调用的格式）
     */
    private Object generateDubboExampleValue(Map<String, Object> definition, 
                                            Map<String, Map<String, Object>> allDefinitions) {
        return generateDubboExampleValue(definition, allDefinitions, new HashSet<>(), 0);
    }
    
    /**
     * 从 Dubbo definition 生成示例值（带循环检测和递归深度限制）
     */
    private Object generateDubboExampleValue(Map<String, Object> definition, 
                                            Map<String, Map<String, Object>> allDefinitions,
                                            Set<String> visited,
                                            int depth) {
        if (definition == null) {
            return new HashMap<>();
        }
        
        if (depth > 10) {
            return new HashMap<>();
        }
        
        Object originalTypeObj = definition.get("originalType");
        String originalType = originalTypeObj != null ? originalTypeObj.toString() : null;
        if (originalType != null && visited.contains(originalType)) {
            return new HashMap<>();
        }
        if (originalType != null) {
            visited.add(originalType);
        }
        
        try {
            Object typeObj = definition.get("type");
            String type = typeObj != null ? typeObj.toString() : null;
            
            if ("array".equals(type)) {
                Object itemsObj = definition.get("items");
                if (itemsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> items = (List<Object>) itemsObj;
                    if (!items.isEmpty()) {
                        String itemType = items.get(0).toString();
                        Object exampleValue = null;
                        
                        if (isBasicType(itemType)) {
                            exampleValue = generateBasicTypeExample(itemType);
                        } else {
                            Map<String, Object> itemDefinition = allDefinitions.get(itemType);
                            if (itemDefinition != null) {
                                exampleValue = generateDubboExampleValue(itemDefinition, allDefinitions, visited, depth + 1);
                            }
                        }
                        
                        if (exampleValue != null) {
                            return Collections.singletonList(exampleValue);
                        }
                    }
                }
                return Collections.emptyList();
            }
            
            if ("object".equals(type) || type == null) {
        Object propertiesObj = definition.get("properties");
        if (propertiesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            Map<String, Object> exampleObj = new HashMap<>();
            
            properties.forEach((propName, propDef) -> {
                if (propDef instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDefMap = (Map<String, Object>) propDef;
                    
                    Object schemaObj = propDefMap.get("schema");
                    if (schemaObj instanceof String) {
                        String schemaRef = (String) schemaObj;
                        if (schemaRef.startsWith("#/x-dubbo-definitions/")) {
                            String definitionKey = schemaRef.substring("#/x-dubbo-definitions/".length());
                            Map<String, Object> nestedDefinition = allDefinitions.get(definitionKey);
                            if (nestedDefinition != null) {
                                        exampleObj.put(propName, generateDubboExampleValue(nestedDefinition, allDefinitions, visited, depth + 1));
                                return;
                            }
                        }
                    }
                    
                            Object propTypeObj = propDefMap.get("type");
                            if (propTypeObj != null) {
                                String propType = propTypeObj.toString();
                        Object exampleValue = null;
                                if (schemaObj == null && propType.contains("<") && propType.contains(">")) {
                                    exampleValue = generateExampleFromGenericType(propType, allDefinitions, visited, depth + 1);
                        }
                        if (exampleValue == null) {
                                    exampleValue = generateBasicTypeExample(propType);
                        }
                        exampleObj.put(propName, exampleValue);
                    } else {
                        exampleObj.put(propName, "");
                    }
                }
            });
            
            return exampleObj;
                }
        }
        
        return new HashMap<>();
        } finally {
            if (originalType != null) {
                visited.remove(originalType);
            }
        }
    }
    
    /**
     * 从泛型类型中提取类型参数并在 x-dubbo-definitions 中查找并解析
     * 例如: "java.util.List<com.spotter.order.api.model.OrderAmzItemDTO>" 
     * 会先查找 "java.util.List<com.spotter.order.api.model.OrderAmzItemDTO>" 的定义
     * 如果找到且 type 为 "array"，从 items 中提取实际类型并解析
     */
    private Object generateExampleFromGenericType(String type, Map<String, Map<String, Object>> definitions) {
        return generateExampleFromGenericType(type, definitions, new HashSet<>(), 0);
    }
    
    /**
     * 从泛型类型中提取类型参数并在 x-dubbo-definitions 中查找并解析（带循环检测和递归深度限制）
     */
    private Object generateExampleFromGenericType(String type, Map<String, Map<String, Object>> definitions,
                                                   Set<String> visited, int depth) {
        if (type == null || !type.contains("<") || !type.contains(">")) {
            return null;
        }
        
        if (depth > 10) {
            return null;
        }
        
        if (visited.contains(type)) {
            return null;
        }
        visited.add(type);
        
        try {
        Map<String, Object> wrapperDefinition = definitions.get(type);
        if (wrapperDefinition != null) {
            Object typeObj = wrapperDefinition.get("type");
            if ("array".equals(typeObj)) {
                Object itemsObj = wrapperDefinition.get("items");
                if (itemsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> items = (List<Object>) itemsObj;
                    if (!items.isEmpty()) {
                        String actualType = items.get(0).toString();
                        Object exampleValue = null;
                        
                        if (isBasicType(actualType)) {
                            exampleValue = generateBasicTypeExample(actualType);
                        } else {
                            Map<String, Object> actualDefinition = definitions.get(actualType);
                            if (actualDefinition != null) {
                                    exampleValue = generateDubboExampleValue(actualDefinition, definitions, visited, depth + 1);
                            }
                        }
                        
                        if (exampleValue != null) {
                            return Collections.singletonList(exampleValue);
                        }
                    }
                }
            }
        }
        
        int startIndex = type.indexOf("<");
        int endIndex = type.lastIndexOf(">");
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return null;
        }
        
        String genericParam = type.substring(startIndex + 1, endIndex).trim();
        String rawType = type.substring(0, startIndex).trim();
        
        Map<String, Object> definition = definitions.get(genericParam);
        if (definition != null) {
                Object exampleValue = generateDubboExampleValue(definition, definitions, visited, depth + 1);
            
            if ("java.util.List".equals(rawType) || "List".equals(rawType)) {
                return Collections.singletonList(exampleValue);
            }
            if ("java.util.Set".equals(rawType) || "Set".equals(rawType)) {
                return Collections.singleton(exampleValue);
            }
            if ("java.util.Map".equals(rawType) || "Map".equals(rawType)) {
                return new HashMap<>();
            }
            
            return exampleValue;
        }
        
        return null;
        } finally {
            visited.remove(type);
        }
    }
    
    /**
     * 判断是否是基本类型
     */
    private boolean isBasicType(String type) {
        if (type == null) {
            return false;
        }
        
        // 基本类型列表
        return type.equals("java.lang.String") || type.equals("String") || type.equals("string")
                || type.equals("java.lang.Integer") || type.equals("Integer") || type.equals("int")
                || type.equals("java.lang.Long") || type.equals("Long") || type.equals("long")
                || type.equals("java.lang.Double") || type.equals("Double") || type.equals("double")
                || type.equals("java.lang.Float") || type.equals("Float") || type.equals("float")
                || type.equals("java.lang.Boolean") || type.equals("Boolean") || type.equals("boolean")
                || type.equals("java.util.List") || type.equals("List")
                || type.equals("java.util.Map") || type.equals("Map")
                || type.equals("java.util.Set") || type.equals("Set");
    }
    
    /**
     * 根据基本类型生成示例值
     */
    private Object generateBasicTypeExample(String type) {
        if (type == null) {
            return "";
        }
        
        // 处理泛型类型，如 List<String>, Map<String, Object>
        if (type.contains("<")) {
            String rawType = type.substring(0, type.indexOf("<"));
            if ("java.util.List".equals(rawType) || "List".equals(rawType)) {
                return Collections.emptyList();
            }
            if ("java.util.Map".equals(rawType) || "Map".equals(rawType)) {
                return new HashMap<>();
            }
            if ("java.util.Set".equals(rawType) || "Set".equals(rawType)) {
                return Collections.emptySet();
            }
        }
        
        // 基本类型
        if (type.equals("java.lang.String") || type.equals("String") || type.equals("string")) {
            return "string";
        }
        if (type.equals("java.lang.Integer") || type.equals("Integer") || type.equals("int")) {
            return 0;
        }
        if (type.equals("java.lang.Long") || type.equals("Long") || type.equals("long")) {
            return 0L;
        }
        if (type.equals("java.lang.Double") || type.equals("Double") || type.equals("double")) {
            return 0.0d;
        }
        if (type.equals("java.lang.Float") || type.equals("Float") || type.equals("float")) {
            return 0.0f;
        }
        if (type.equals("java.lang.Boolean") || type.equals("Boolean") || type.equals("boolean")) {
            return false;
        }
        if (type.equals("java.util.List") || type.equals("List")) {
            return Collections.emptyList();
        }
        if (type.equals("java.util.Map") || type.equals("Map")) {
            return new HashMap<>();
        }
        if (type.equals("java.util.Set") || type.equals("Set")) {
            return Collections.emptySet();
        }
        
        // 默认返回空字符串
        return "";
    }
    
    /**
     * 解析 Dubbo 定义（递归处理嵌套的 schema 引用）
     */
    private Map<String, Object> parseDubboDefinition(Map<String, Object> definition, 
                                                      Map<String, Map<String, Object>> allDefinitions) {
        if (definition == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // 复制基本信息
        Object typeObj = definition.get("type");
        if (typeObj != null) {
            result.put("type", typeObj);
        }
        
        Object originalTypeObj = definition.get("originalType");
        if (originalTypeObj != null) {
            result.put("originalType", originalTypeObj);
        }
        
        // 解析 properties
        Object propertiesObj = definition.get("properties");
        if (propertiesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            Map<String, Object> parsedProperties = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();
                
                if (propValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDef = (Map<String, Object>) propValue;
                    
                    // 检查是否有 schema 引用
                    Object schemaObj = propDef.get("schema");
                    if (schemaObj instanceof String) {
                        String schemaRef = (String) schemaObj;
                        if (schemaRef.startsWith("#/x-dubbo-definitions/")) {
                            String definitionKey = schemaRef.substring("#/x-dubbo-definitions/".length());
                            Map<String, Object> nestedDef = allDefinitions.get(definitionKey);
                            if (nestedDef != null) {
                                parsedProperties.put(propName, parseDubboDefinition(nestedDef, allDefinitions));
                            } else {
                                // 如果找不到定义，保留原始信息
                                Map<String, Object> propInfo = new HashMap<>();
                                propInfo.put("type", propDef.get("type"));
                                parsedProperties.put(propName, propInfo);
                            }
                        } else {
                            parsedProperties.put(propName, propDef);
                        }
                    } else {
                        // 没有 schema 引用，直接使用
                        parsedProperties.put(propName, propDef);
                    }
                } else {
                    parsedProperties.put(propName, propValue);
                }
            }
            
            result.put("properties", parsedProperties);
        }
        
        // 处理 enum
        Object enumObj = definition.get("enum");
        if (enumObj instanceof List) {
            result.put("enum", enumObj);
        }
        
        // 处理 items（数组类型）
        Object itemsObj = definition.get("items");
        if (itemsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) itemsObj;
            List<Object> parsedItems = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof String) {
                    parsedItems.add(item);
                }
            }
            result.put("items", parsedItems);
        }
        
        return result;
    }
    
    /**
     * 从 interfaceName 中提取最后一个 . 后的部分作为模块名
     * 例如: "com.spotter.order.web.api.IFulfillConfigEndpoint" -> "IFulfillConfigEndpoint"
     */
    private String extractModuleNameFromInterface(String interfaceName) {
        if (StringUtils.isBlank(interfaceName)) {
            return "Default";
        }
        int lastDotIndex = interfaceName.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < interfaceName.length() - 1) {
            return interfaceName.substring(lastDotIndex + 1);
        }
        return interfaceName;
    }
    
    /**
     * 从路径中提取第一个 / 后的第一部分作为模块名
     * 例如: "/fulfill/cancel/audit" -> "fulfill"
     */
    private String extractModuleNameFromPath(String path) {
        if (StringUtils.isBlank(path)) {
            return "Default";
        }
        // 移除开头的 /，然后取第一个 / 前的部分
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        if (StringUtils.isBlank(cleanPath)) {
            return "Default";
        }
        int firstSlashIndex = cleanPath.indexOf('/');
        if (firstSlashIndex > 0) {
            return cleanPath.substring(0, firstSlashIndex);
        }
        return cleanPath;
    }
    
    /**
     * 从 Swagger URL 中提取网关路径
     * 例如: "http://api.tst.spotterio.com/spotter-evidence-web/swagger/doc" -> "/spotter-evidence-web"
     */
    private String extractGatewayPathFromUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (StringUtils.isBlank(path) || path.equals("/")) {
                return null;
            }
            
            // 移除开头的 /，然后分割路径段
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            if (StringUtils.isBlank(cleanPath)) {
                return null;
            }
            
            String[] segments = cleanPath.split("/");
            if (segments.length == 0) {
                return null;
            }
            
            // 跳过常见的 Swagger 文档路径段
            Set<String> swaggerKeywords = Set.of("swagger", "doc", "api-docs", "v2", "v3", "openapi", "api");
            
            // 查找第一个不是 Swagger 关键词的路径段作为网关路径
            for (String segment : segments) {
                if (StringUtils.isNotBlank(segment) && !swaggerKeywords.contains(segment.toLowerCase())) {
                    return "/" + segment;
                }
            }
            
            // 如果没有找到，返回 null
            return null;
        } catch (Exception e) {
            LogUtils.warn("从 URL 提取网关路径失败: " + url + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从路径中提取最后一个 / 后的部分作为 name
     * 例如: "/fulfill/cancel/audit" -> "audit"
     */
    private String extractNameFromPath(String path) {
        if (StringUtils.isBlank(path)) {
            return "default";
        }
        // 移除末尾的 /
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        if (StringUtils.isBlank(cleanPath)) {
            return "default";
        }
        int lastSlashIndex = cleanPath.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < cleanPath.length() - 1) {
            return cleanPath.substring(lastSlashIndex + 1);
        }
        // 如果没有 /，返回整个路径（去掉开头的 /）
        return cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath;
    }
    
    /**
     * 查找或创建模块
     * 在指定的父模块下查找指定名称的模块，如果不存在则创建
     */
    private String findOrCreateModule(String projectId, String parentId, String moduleName, String userId, String moduleType) {
        // 查询父模块下的所有子模块
        List<io.vanguard.testops.metadata.domain.MetadataModule> modules = metadataModuleMapper.selectByProjectId(projectId);
        
        // 查找是否存在同名的子模块（在指定父模块下）
        for (io.vanguard.testops.metadata.domain.MetadataModule module : modules) {
            if (moduleName.equals(module.getName()) && parentId.equals(module.getParentId())) {
                return module.getId();
            }
        }
        
        // 如果不存在，创建新模块
        MetadataModuleCreateRequest createRequest = new MetadataModuleCreateRequest();
        createRequest.setProjectId(projectId);
        createRequest.setParentId(parentId);
        createRequest.setName(moduleName);
        createRequest.setModuleType(moduleType);
        
        return metadataModuleService.create(createRequest, userId);
    }

    /**
     * 导入数据库表DDL（异步执行）
     */
    @Async("metadataImportExecutor")
    public void importDatabaseTable(DatabaseTableImportRequest request, String userId) {
        Connection connection = null;
        
        try {
            Map<String, Object> dataEndpoint = request.getDataEndpoint();
            if (dataEndpoint == null) {
                throw new MSException("数据库连接信息不能为空");
            }
            
            String host = getStringValue(dataEndpoint, "host");
            String port = getStringValue(dataEndpoint, "port");
            String user = getStringValue(dataEndpoint, "user");
            String password = getStringValue(dataEndpoint, "password");
            
            if (StringUtils.isBlank(host) || StringUtils.isBlank(port) || StringUtils.isBlank(user)) {
                throw new MSException("数据库连接信息不完整，需要host、port、user");
            }
            
            String driverClassName = "com.mysql.cj.jdbc.Driver";
            String dbUrl = String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, request.getDatabase());
            
            Class.forName(driverClassName);
            
            connection = DriverManager.getConnection(dbUrl, user, password != null ? password : "");
            
            io.vanguard.testops.metadata.domain.MetadataModule module = metadataModuleMapper.selectByIdWithTimestamp(request.getModuleId());
            if (module == null) {
                throw new MSException("模块不存在: " + request.getModuleId());
            }
            
            String projectId = request.getProjectId();
            if (!projectId.equals(module.getProjectId())) {
                throw new MSException("模块ID与项目ID不匹配");
            }
            
            String databaseModuleId = findOrCreateModule(projectId, request.getModuleId(), request.getDatabase(), userId, "SQL");
            
            List<String> existInfo = new ArrayList<>();
            List<MetadataDefinition> importList = new ArrayList<>();
            
            if (StringUtils.isNotBlank(request.getTableName())) {
                importSingleTable(connection, request, projectId, databaseModuleId, userId, existInfo, importList, dbUrl, driverClassName, dataEndpoint);
            } else {
                importAllTables(connection, request, projectId, databaseModuleId, userId, existInfo, importList, dbUrl, driverClassName, dataEndpoint);
            }
            
            LogUtils.info("异步导入数据库表DDL完成: 已存在 {} 个表, 导入 {} 个表", existInfo.size(), importList.size());
            
        } catch (ClassNotFoundException e) {
            LogUtils.error("异步导入数据库表DDL失败: 数据库驱动类未找到", e);
        } catch (SQLException e) {
            LogUtils.error("异步导入数据库表DDL失败: 数据库连接或查询失败", e);
        } catch (MSException e) {
            LogUtils.error("异步导入数据库表DDL失败: " + e.getMessage(), e);
        } catch (Exception e) {
            LogUtils.error("异步导入数据库表DDL失败", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LogUtils.error("关闭Connection失败", e);
                }
            }
        }
    }
    
    /**
     * 导入单个表
     */
    private void importSingleTable(Connection connection, DatabaseTableImportRequest request, String projectId, 
                                   String databaseModuleId, String userId, List<String> existInfo, List<MetadataDefinition> importList,
                                   String dbUrl, String driverClassName, Map<String, Object> dataEndpoint) throws SQLException {
        String ddl = getTableDDL(connection, request.getDatabase(), request.getTableName());
        
        if (StringUtils.isBlank(ddl)) {
            throw new MSException("无法获取表DDL: " + request.getTableName());
        }
        
        List<MetadataDefinition> existingDefinitions = metadataDefinitionMapper.selectByProjectId(projectId).stream()
                .filter(def -> "SQL".equals(def.getProtocol()))
                .filter(def -> request.getTableName().equals(def.getName()))
                .filter(def -> databaseModuleId.equals(def.getModuleId()))
                .collect(Collectors.toList());
        
        long currentTime = System.currentTimeMillis();
        
        if (!existingDefinitions.isEmpty()) {
            MetadataDefinition existing = existingDefinitions.get(0);
            existing.setScriptContent(ddl);
            existing.setUpdateTime(currentTime);
            metadataDefinitionMapper.updateById(existing);
            importList.add(existing);
        } else {
            String id = IDGenerator.nextStr();
            
            MetadataDefinition definition = new MetadataDefinition();
            definition.setId(id);
            definition.setProjectId(projectId);
            definition.setModuleId(databaseModuleId);
            definition.setName(request.getTableName());
            definition.setProtocol("SQL");
            definition.setVersion(1);
            definition.setIsLatest(true);
            definition.setScriptContent(ddl);
            
            definition.setCreateUser(userId);
            definition.setCreateTime(currentTime);
            definition.setUpdateTime(currentTime);
            
            metadataDefinitionMapper.insert(definition);
            importList.add(definition);
        }
    }
    
    /**
     * 导入数据库所有表
     */
    private void importAllTables(Connection connection, DatabaseTableImportRequest request, String projectId,
                                String databaseModuleId, String userId, List<String> existInfo, List<MetadataDefinition> importList,
                                String dbUrl, String driverClassName, Map<String, Object> dataEndpoint) throws SQLException {
        List<String> tableNames = getAllTableNames(connection, request.getDatabase());
        
        if (tableNames.isEmpty()) {
            throw new MSException("数据库 " + request.getDatabase() + " 中没有找到任何表");
        }
        
        List<MetadataDefinition> existingDefinitions = metadataDefinitionMapper.selectByProjectId(projectId).stream()
                .filter(def -> "SQL".equals(def.getProtocol()))
                .filter(def -> databaseModuleId.equals(def.getModuleId()))
                .collect(Collectors.toList());
        
        Map<String, MetadataDefinition> existingMap = existingDefinitions.stream()
                .collect(Collectors.toMap(MetadataDefinition::getName, def -> def, (v1, v2) -> v1));
        
        long currentTime = System.currentTimeMillis();
        
        for (String tableName : tableNames) {
            String ddl = getTableDDL(connection, request.getDatabase(), tableName);
            if (StringUtils.isBlank(ddl)) {
                LogUtils.warn("无法获取表DDL: " + tableName);
                continue;
            }
            
            MetadataDefinition existing = existingMap.get(tableName);
            if (existing != null) {
                existing.setScriptContent(ddl);
                existing.setUpdateTime(currentTime);
                metadataDefinitionMapper.updateById(existing);
                importList.add(existing);
            } else {
                String id = IDGenerator.nextStr();
                
                MetadataDefinition definition = new MetadataDefinition();
                definition.setId(id);
                definition.setProjectId(projectId);
                definition.setModuleId(databaseModuleId);
                definition.setName(tableName);
                definition.setProtocol("SQL");
                definition.setVersion(1);
                definition.setIsLatest(true);
                definition.setScriptContent(ddl);
                
                definition.setCreateUser(userId);
                definition.setCreateTime(currentTime);
                definition.setUpdateTime(currentTime);
                
                metadataDefinitionMapper.insert(definition);
                importList.add(definition);
            }
        }
    }
    
    /**
     * 获取数据库所有表名
     */
    private List<String> getAllTableNames(Connection connection, String database) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement()) {
            if (StringUtils.isNotBlank(database)) {
                stmt.execute("USE `" + database + "`");
            }
            
            String sql = "SHOW TABLES";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    if (StringUtils.isNotBlank(tableName)) {
                        tableNames.add(tableName);
                    }
                }
            }
        }
        
        return tableNames;
    }
    
    /**
     * 从Map中获取String值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
    
    /**
     * 获取表的DDL语句
     */
    private String getTableDDL(Connection connection, String database, String tableName) throws SQLException {
        String ddl = null;
        
        try (Statement stmt = connection.createStatement()) {
            if (StringUtils.isNotBlank(database)) {
                stmt.execute("USE `" + database + "`");
            }
            
            String sql = "SHOW CREATE TABLE `" + tableName + "`";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String columnName = "Create Table";
                    try {
                        ddl = rs.getString(columnName);
                    } catch (SQLException e) {
                        ddl = rs.getString(2);
                    }
                }
            }
        }
        
        return ddl;
    }
}
