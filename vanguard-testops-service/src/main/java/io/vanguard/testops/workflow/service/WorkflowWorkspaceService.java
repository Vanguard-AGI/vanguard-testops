package io.vanguard.testops.workflow.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.metadata.domain.MetadataModule;
import io.vanguard.testops.metadata.mapper.MetadataModuleMapper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.workflow.domain.WorkflowDefinition;
import io.vanguard.testops.workflow.domain.WorkflowWorkspace;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.mapper.WorkflowDefinitionMapper;
import io.vanguard.testops.workflow.mapper.WorkflowWorkspaceMapper;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import io.vanguard.testops.workflow.support.cache.WorkflowWorkspaceCache;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流工作空间服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowWorkspaceService {

    @Resource
    private WorkflowWorkspaceMapper workflowWorkspaceMapper;

    @Resource
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Resource
    private MetadataModuleMapper metadataModuleMapper;

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    @Resource
    private WorkflowDefinitionService workflowDefinitionService;

    @Resource
    private io.vanguard.testops.project.service.ProjectService projectService;

    @Resource
    private WorkflowWorkspaceCache workflowWorkspaceCache;

    /**
     * 创建工作空间
     */
    public WorkflowWorkspaceDTO create(WorkflowWorkspaceCreateRequest request, String userId) {
        long currentTime = System.currentTimeMillis();

        WorkflowWorkspace workspace = new WorkflowWorkspace();
        workspace.setWorkspaceId(IDGenerator.nextStr());
        workspace.setWorkspaceName(request.getName());
        workspace.setProjectId(request.getProjectId());
        workspace.setOwner(request.getResponsiblePerson());
        workspace.setDescription(request.getDescription());
        workspace.setIcon(StringUtils.defaultIfBlank(request.getIcon(), "📁"));
        workspace.setIconColor(StringUtils.defaultIfBlank(request.getIconColor(), "bg-gray-100"));
        workspace.setVisibility(StringUtils.defaultIfBlank(request.getVisibility(), "PRIVATE"));
        workspace.setGlobalVars(new HashMap<>());
        workspace.setCreateTime(currentTime);
        workspace.setCreateUser(userId);
        workspace.setUpdateTime(currentTime);
        workspace.setUpdateUser(userId);

        workflowWorkspaceMapper.insert(workspace);

        // 清除缓存
        workflowWorkspaceCache.evictByProjectId(request.getProjectId());

        // ⭐ 自动为工作空间创建一个默认的根模块
        // 数据结构：项目 -> 空间 -> 模块
        // project_id 使用项目的ID（用于数据隔离）
        // type_id 当 module_type='WORKFLOW' 时存储 workspaceId，用于关联工作空间
        MetadataModule defaultModule = new MetadataModule();
        String defaultModuleId = IDGenerator.nextStr();
        defaultModule.setId(defaultModuleId);
        defaultModule.setProjectId(request.getProjectId()); // ⭐ 使用项目ID（数据隔离）
        defaultModule.setTypeId(workspace.getWorkspaceId()); // ⭐ 当module_type='WORKFLOW'时，type_id存储workspaceId
        defaultModule.setParentId("ROOT");
        defaultModule.setName("默认模块"); // 默认模块名称
        defaultModule.setModuleType("WORKFLOW"); // 工作流类型的模块
        defaultModule.setPos(0L);
        defaultModule.setCreateTime(currentTime);
        defaultModule.setUpdateTime(currentTime);
        defaultModule.setDeletedTime(null);
        metadataModuleMapper.insert(defaultModule);

        // ⭐ 在默认模块下创建"工作流同步"子模块
        MetadataModule syncModule = new MetadataModule();
        syncModule.setId(IDGenerator.nextStr());
        syncModule.setProjectId(request.getProjectId());
        syncModule.setTypeId(workspace.getWorkspaceId());
        syncModule.setParentId(defaultModuleId); // 父模块为默认模块
        syncModule.setName("工作流同步");
        syncModule.setModuleType("WORKFLOW");
        syncModule.setPos(0L);
        syncModule.setCreateTime(currentTime);
        syncModule.setUpdateTime(currentTime);
        syncModule.setDeletedTime(null);
        metadataModuleMapper.insert(syncModule);

        return convertToDTO(workspace);
    }

    /**
     * 更新工作空间
     */
    public WorkflowWorkspaceDTO update(WorkflowWorkspaceUpdateRequest request, String userId) {
        WorkflowWorkspace existing = workflowWorkspaceMapper.selectByWorkspaceId(request.getId());
        if (existing == null) {
            throw new MSException("工作空间不存在");
        }

        if (StringUtils.isNotBlank(request.getName())) {
            existing.setWorkspaceName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getResponsiblePerson())) {
            existing.setOwner(request.getResponsiblePerson());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (StringUtils.isNotBlank(request.getIcon())) {
            existing.setIcon(request.getIcon());
        }
        if (StringUtils.isNotBlank(request.getIconColor())) {
            existing.setIconColor(request.getIconColor());
        }
        if (StringUtils.isNotBlank(request.getVisibility())) {
            existing.setVisibility(request.getVisibility());
        }

        existing.setUpdateTime(System.currentTimeMillis());
        existing.setUpdateUser(userId);
        workflowWorkspaceMapper.updateById(existing);

        // 清除缓存
        workflowWorkspaceCache.evictByWorkspaceId(request.getId(), existing.getProjectId());

        return convertToDTO(existing);
    }

    /**
     * 获取工作空间详情
     */
    public WorkflowWorkspaceDTO get(String workspaceId) {
        WorkflowWorkspace workspace = workflowWorkspaceMapper.selectByWorkspaceId(workspaceId);
        if (workspace == null) {
            throw new MSException("工作空间不存在");
        }

        WorkflowWorkspaceDTO dto = convertToDTO(workspace);

        // 加载统计信息
        loadStatistics(dto, workspace);

        return dto;
    }

    /**
     * 获取工作空间列表（按项目ID）
     */
    public List<WorkflowWorkspaceDTO> getListByProject(String projectId, String keyword) {
        // 尝试从缓存获取
        List<WorkflowWorkspaceDTO> cachedList = workflowWorkspaceCache.get(projectId, keyword);
        if (cachedList != null) {
            return cachedList;
        }

        // 缓存未命中，从数据库查询
        // 关键词过滤已在 SQL 层面完成，无需在内存中过滤
        List<WorkflowWorkspace> workspaces = workflowWorkspaceMapper.selectByProjectId(projectId, keyword);

        if (CollectionUtils.isEmpty(workspaces)) {
            return new ArrayList<>();
        }

        List<WorkflowWorkspaceDTO> dtoList = workspaces.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 批量加载所有空间的统计信息（性能优化：避免 N+1 查询）
        if (CollectionUtils.isNotEmpty(dtoList)) {
            loadStatisticsBatch(dtoList, workspaces, projectId);
        }
        // 写入缓存
        workflowWorkspaceCache.set(projectId, keyword, dtoList);

        return dtoList;
    }

    /**
     * 根据当前登录用户查询可访问的工作空间列表（包含工作流同步模块ID）
     */
    public List<WorkflowWorkspaceDTO> getListByUser(String userId, String organizationId) {
        // 1. 查询用户所属的所有项目
        List<io.vanguard.testops.project.domain.Project> userProjects = projectService.getUserProject(organizationId, userId);
        
        if (CollectionUtils.isEmpty(userProjects)) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有项目ID
        List<String> projectIds = userProjects.stream()
                .map(io.vanguard.testops.project.domain.Project::getId)
                .collect(Collectors.toList());
        
        // 3. 查询这些项目下的所有工作空间
        List<WorkflowWorkspace> allWorkspaces = new ArrayList<>();
        for (String projectId : projectIds) {
            List<WorkflowWorkspace> workspaces = workflowWorkspaceMapper.selectByProjectId(projectId, null);
            if (CollectionUtils.isNotEmpty(workspaces)) {
                allWorkspaces.addAll(workspaces);
            }
        }
        
        if (CollectionUtils.isEmpty(allWorkspaces)) {
            return new ArrayList<>();
        }
        
        // 4. 转换为DTO并加载统计信息
        List<WorkflowWorkspaceDTO> dtoList = allWorkspaces.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // 5. 为每个工作空间加载统计信息和查询工作流同步模块ID
        for (WorkflowWorkspaceDTO dto : dtoList) {
            WorkflowWorkspace ws = allWorkspaces.stream()
                    .filter(w -> w.getWorkspaceId().equals(dto.getWorkspaceId()))
                    .findFirst()
                    .orElse(null);
            if (ws != null) {
                loadStatistics(dto, ws);
                
                // 6. 查询工作流同步模块ID
                List<MetadataModule> modules = metadataModuleMapper.selectByProjectIdAndTypeId(
                        dto.getProjectId(), dto.getWorkspaceId(), "WORKFLOW");
                if (CollectionUtils.isNotEmpty(modules)) {
                    // 查找名为"工作流同步"的模块
                    MetadataModule syncModule = modules.stream()
                            .filter(m -> "工作流同步".equals(m.getName()))
                            .findFirst()
                            .orElse(null);
                    if (syncModule != null) {
                        dto.setSyncModuleId(syncModule.getId());
                    }
                }
            }
        }
        
        // 按创建时间倒序排序
        dtoList.sort((a, b) -> {
            Long timeA = a.getCreateTime() != null ? a.getCreateTime() : 0L;
            Long timeB = b.getCreateTime() != null ? b.getCreateTime() : 0L;
            return timeB.compareTo(timeA);
        });
        
        return dtoList;
    }

    /**
     * 工作流同步导入：将HTTP和SQL请求转换为工作流节点并导入
     */
    public WorkflowSyncImportResponse syncImport(WorkflowSyncImportRequest request, String userId) {
        // 1. 根据workspaceId查询工作流同步模块ID
        List<MetadataModule> modules = metadataModuleMapper.selectByProjectIdAndTypeId(
                request.getProjectId(), request.getWorkspaceId(), "WORKFLOW");
        if (CollectionUtils.isEmpty(modules)) {
            throw new MSException("工作空间下没有找到模块");
        }
        
        MetadataModule syncModule = modules.stream()
                .filter(m -> "工作流同步".equals(m.getName()))
                .findFirst()
                .orElse(null);
        
        if (syncModule == null) {
            throw new MSException("工作空间下没有找到\"工作流同步\"模块");
        }
        
        String syncModuleId = syncModule.getId();
        
        // 2. 将HTTP和SQL请求转换为WorkflowNodeDTO格式
        List<WorkflowNodeDTO> workflowNodes = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < request.getNodes().size(); i++) {
            WorkflowSyncImportRequest.SyncNodeData nodeData = request.getNodes().get(i);
            try {
                WorkflowNodeDTO node = convertSyncNodeToWorkflowNode(nodeData, i);
                workflowNodes.add(node);
                successCount++;
            } catch (Exception e) {
                LogUtils.error("转换节点失败: " + e.getMessage(), e);
                failCount++;
            }
        }
        
        if (CollectionUtils.isEmpty(workflowNodes)) {
            throw new MSException("没有成功转换的节点");
        }
        
        // 3. 生成工作流名称（如果没有提供）
        String workflowName = StringUtils.isNotBlank(request.getWorkflowName()) 
                ? request.getWorkflowName() 
                : "从插件导入的工作流_" + System.currentTimeMillis();
        
        // 4. 根据节点类型确定工作流分类（工作流分类：API/UI/AGENT）
        String category = "API"; // 默认分类为API（HTTP和SQL都归类为API）
        
        // 5. 按节点顺序生成连接线
        List<WorkflowConnectionDTO> connections = new ArrayList<>();
        for (int i = 0; i < workflowNodes.size() - 1; i++) {
            WorkflowNodeDTO sourceNode = workflowNodes.get(i);
            WorkflowNodeDTO targetNode = workflowNodes.get(i + 1);
            
            WorkflowConnectionDTO connection = new WorkflowConnectionDTO();
            connection.setFrom(sourceNode.getId());
            connection.setTo(targetNode.getId());
            connection.setOrderNum(i);
            connection.setLabel(""); // 默认空标签
            // 使用上一个节点（源节点）的颜色
            connection.setColor(getNodeColor(sourceNode.getType()));
            connections.add(connection);
        }
        
        // 6. 创建WorkflowDefinitionSaveRequest
        WorkflowDefinitionSaveRequest saveRequest = new WorkflowDefinitionSaveRequest();
        saveRequest.setProjectId(request.getProjectId());
        saveRequest.setModuleId(syncModuleId);
        saveRequest.setName(workflowName);
        saveRequest.setDescription(request.getDescription());
        saveRequest.setCategory(category);
        saveRequest.setType("TEST_CASE");
        saveRequest.setNodes(workflowNodes);
        saveRequest.setConnections(connections);
        
        // 7. 保存工作流
        String workflowId = workflowDefinitionService.save(saveRequest, userId);
        
        // 8. 构建响应
        WorkflowSyncImportResponse response = new WorkflowSyncImportResponse();
        response.setWorkflowId(workflowId);
        response.setWorkflowName(workflowName);
        response.setModuleId(syncModuleId);
        response.setNodeCount(request.getNodes().size());
        response.setSuccessCount(successCount);
        response.setFailCount(failCount);
        
        return response;
    }
    
    /**
     * 将同步节点数据转换为工作流节点DTO
     */
    private WorkflowNodeDTO convertSyncNodeToWorkflowNode(WorkflowSyncImportRequest.SyncNodeData nodeData, int index) {
        WorkflowNodeDTO node = new WorkflowNodeDTO();
        node.setId(IDGenerator.nextStr());
        node.setType(nodeData.getType());
        node.setEnable(true);
        node.setRefMode("NONE");
        
        // 设置节点坐标：x = 100 + index * 200, y = 100
        node.setX(100.0 + index * 200.0);
        node.setY(100.0);
        node.setOrderNum((long) index);
        
        // 构建节点配置
        Map<String, Object> config = new HashMap<>();
        
        if ("http_request".equals(nodeData.getType())) {
            // HTTP节点配置
            if (StringUtils.isNotBlank(nodeData.getMethod())) {
                config.put("method", nodeData.getMethod());
            }
            if (StringUtils.isNotBlank(nodeData.getUrl())) {
                config.put("url", nodeData.getUrl());
            }
            if (StringUtils.isNotBlank(nodeData.getPath())) {
                config.put("path", nodeData.getPath());
            }
            if (nodeData.getHeaders() != null && !nodeData.getHeaders().isEmpty()) {
                config.put("headers", nodeData.getHeaders());
            }
            
            // 处理请求体参数：统一转换为json格式
            // 优先级：json > data > queryParams > body
            Object jsonData = null;
            if (nodeData.getJson() != null) {
                // 已有JSON数据，直接使用
                jsonData = nodeData.getJson();
            } else if (nodeData.getData() != null && !nodeData.getData().isEmpty()) {
                // 表单数据转换为JSON对象
                jsonData = nodeData.getData();
            } else if (nodeData.getQueryParams() != null && !nodeData.getQueryParams().isEmpty()) {
                // 查询参数转换为JSON对象
                jsonData = nodeData.getQueryParams();
            } else if (nodeData.getBody() != null) {
                // 原始body是String类型，尝试解析为JSON对象，如果失败则作为字符串使用
                String bodyStr = nodeData.getBody();
                try {
                    // 尝试解析为JSON对象
                    jsonData = JSON.parseObject(bodyStr, Map.class);
                } catch (Exception e) {
                    // 解析失败，作为字符串对象使用
                    jsonData = bodyStr;
                }
            }
            
            // 统一设置为json类型
            if (jsonData != null) {
                config.put("json", jsonData);
            }
            config.put("paramType", "json");
            
            // 生成节点名称
            if (StringUtils.isNotBlank(nodeData.getName())) {
                node.setName(nodeData.getName());
            } else {
                String method = StringUtils.isNotBlank(nodeData.getMethod()) ? nodeData.getMethod() : "GET";
                String path = StringUtils.isNotBlank(nodeData.getPath()) ? nodeData.getPath() : 
                             (StringUtils.isNotBlank(nodeData.getUrl()) ? nodeData.getUrl() : "HTTP请求");
                node.setName(method + " " + path);
            }
        } else if ("mysql".equals(nodeData.getType()) || "sql".equals(nodeData.getType())) {
            // SQL节点配置：
            node.setType("mysql");
            if (StringUtils.isNotBlank(nodeData.getSql())) {
                config.put("sql", nodeData.getSql());
            }
            
            // 将operationType（大写）转换为operation（小写）
            String operation = "select"; // 默认值
            if (StringUtils.isNotBlank(nodeData.getOperationType())) {
                String operationType = nodeData.getOperationType().toLowerCase();
                // 支持SELECT/INSERT/UPDATE/DELETE/EXECUTE
                if ("select".equals(operationType) || "insert".equals(operationType) || 
                    "update".equals(operationType) || "delete".equals(operationType) || 
                    "execute".equals(operationType)) {
                    operation = operationType;
                } else {
                    // 如果传入的是大写，转换为小写
                    operation = operationType.toLowerCase();
                }
            }
            config.put("operation", operation);
            
            // 设置默认的connection配置
            Map<String, Object> connection = new HashMap<>();
            connection.put("host", "localhost");
            connection.put("port", 3306);
            connection.put("charset", "utf8mb4");
            connection.put("connect_timeout", 10);
            connection.put("read_timeout", 30);
            connection.put("write_timeout", 30);
            config.put("connection", connection);
            
            // 设置默认的environmentVariables（空Map，前端会根据环境配置填充）
            config.put("environmentVariables", new HashMap<String, String>());
            
            // 生成节点名称
            if (StringUtils.isNotBlank(nodeData.getName())) {
                node.setName(nodeData.getName());
            } else {
                String operationType = StringUtils.isNotBlank(nodeData.getOperationType()) 
                        ? nodeData.getOperationType() : "SQL";
                node.setName(operationType + " 查询");
            }
        } else {
            throw new MSException("不支持的节点类型: " + nodeData.getType());
        }
        
        node.setConfig(config);
        return node;
    }

    /**
     * 复制工作空间
     */
    public WorkflowWorkspaceDTO copy(String workspaceId, String userId) {
        WorkflowWorkspace source = workflowWorkspaceMapper.selectByWorkspaceId(workspaceId);
        if (source == null) {
            throw new MSException("工作空间不存在");
        }

        long currentTime = System.currentTimeMillis();

        // 创建新的工作空间
        WorkflowWorkspace workspace = new WorkflowWorkspace();
        workspace.setWorkspaceId(IDGenerator.nextStr());
        workspace.setWorkspaceName(source.getWorkspaceName() + "_copy");
        workspace.setProjectId(source.getProjectId());
        workspace.setOwner(source.getOwner());
        workspace.setDescription(source.getDescription());
        workspace.setIcon(source.getIcon());
        workspace.setIconColor(source.getIconColor());
        workspace.setVisibility(source.getVisibility());
        workspace.setGlobalVars(source.getGlobalVars() != null ? new HashMap<>(source.getGlobalVars()) : new HashMap<>());
        workspace.setCreateTime(currentTime);
        workspace.setCreateUser(userId);
        workspace.setUpdateTime(currentTime);
        workspace.setUpdateUser(userId);

        workflowWorkspaceMapper.insert(workspace);

        // 清除缓存
        workflowWorkspaceCache.evictByProjectId(source.getProjectId());

        // ⭐ 复制原空间的模块（创建新模块）
        // 需要建立源模块ID到新模块ID的映射，以便正确处理父子关系
        List<MetadataModule> sourceModules = metadataModuleMapper.selectByProjectIdAndTypeId(
                source.getProjectId(), source.getWorkspaceId(), "WORKFLOW");
        if (CollectionUtils.isNotEmpty(sourceModules)) {
            // 建立源模块ID到新模块ID的映射
            Map<String, String> moduleIdMapping = new HashMap<>();
            
            // 按层级顺序创建模块：先创建父模块，再创建子模块
            // 使用循环处理多层嵌套的情况
            boolean hasMoreModules = true;
            while (hasMoreModules) {
                hasMoreModules = false;
                for (MetadataModule sourceModule : sourceModules) {
                    // 如果该模块已经创建过，跳过
                    if (moduleIdMapping.containsKey(sourceModule.getId())) {
                        continue;
                    }
                    
                    // 判断父模块是否已创建（parentId="ROOT" 或 parentId 已在映射中）
                    String sourceParentId = sourceModule.getParentId();
                    String newParentId = null;
                    if ("ROOT".equals(sourceParentId)) {
                        newParentId = "ROOT";
                    } else {
                        newParentId = moduleIdMapping.get(sourceParentId);
                        if (newParentId == null) {
                            // 父模块还未创建，跳过，等待下一轮
                            hasMoreModules = true;
                            continue;
                        }
                    }
                    
                    // 创建新模块
                    String newModuleId = IDGenerator.nextStr();
                    MetadataModule newModule = new MetadataModule();
                    newModule.setId(newModuleId);
                    newModule.setProjectId(workspace.getProjectId());
                    newModule.setTypeId(workspace.getWorkspaceId()); // 关联到新空间
                    newModule.setParentId(newParentId);
                    newModule.setName(sourceModule.getName());
                    newModule.setModuleType(sourceModule.getModuleType());
                    newModule.setPos(sourceModule.getPos());
                    newModule.setCreateTime(currentTime);
                    newModule.setUpdateTime(currentTime);
                    newModule.setDeletedTime(null);
                    metadataModuleMapper.insert(newModule);
                    
                    // 建立映射关系
                    moduleIdMapping.put(sourceModule.getId(), newModuleId);
                    hasMoreModules = true; // 标记有新模块创建，继续下一轮
                }
            }
            
            // ⭐ 复制每个模块下的工作流用例
            for (MetadataModule sourceModule : sourceModules) {
                String newModuleId = moduleIdMapping.get(sourceModule.getId());
                if (newModuleId == null) {
                    continue; // 如果模块映射不存在，跳过
                }
                
                // 查询源模块下的所有工作流
                List<WorkflowDefinition> sourceWorkflows = workflowDefinitionMapper.selectByModuleId(sourceModule.getId());
                if (CollectionUtils.isNotEmpty(sourceWorkflows)) {
                    for (WorkflowDefinition sourceWorkflow : sourceWorkflows) {
                        try {
                            // 获取工作流详情
                            WorkflowDefinitionDTO workflowDTO = workflowDefinitionService.get(sourceWorkflow.getWorkflowId());
                            
                            // 创建新的保存请求
                            WorkflowDefinitionSaveRequest saveRequest = new WorkflowDefinitionSaveRequest();
                            saveRequest.setProjectId(workspace.getProjectId());
                            saveRequest.setModuleId(newModuleId); // 使用新模块ID
                            saveRequest.setName(workflowDTO.getName() + "_copy");
                            saveRequest.setDescription(workflowDTO.getDescription());
                            saveRequest.setCategory(workflowDTO.getCategory());
                            saveRequest.setType(workflowDTO.getType());
                            saveRequest.setGlobalVars(workflowDTO.getGlobalVars());
                            
                            // 为节点生成新的ID，并建立旧ID到新ID的映射
                            Map<String, String> nodeIdMapping = new HashMap<>();
                            List<WorkflowNodeDTO> newNodes = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(workflowDTO.getNodes())) {
                                for (WorkflowNodeDTO node : workflowDTO.getNodes()) {
                                    String oldId = node.getId();
                                    String newId = IDGenerator.nextStr();
                                    nodeIdMapping.put(oldId, newId);
                                    
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
                            saveRequest.setNodes(newNodes);
                            
                            // 更新连线中的节点引用
                            List<WorkflowConnectionDTO> newConnections = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(workflowDTO.getConnections())) {
                                for (WorkflowConnectionDTO conn : workflowDTO.getConnections()) {
                                    WorkflowConnectionDTO newConn = new WorkflowConnectionDTO();
                                    newConn.setFrom(nodeIdMapping.getOrDefault(conn.getFrom(), conn.getFrom()));
                                    newConn.setTo(nodeIdMapping.getOrDefault(conn.getTo(), conn.getTo()));
                                    newConn.setLabel(conn.getLabel());
                                    newConn.setColor(conn.getColor());
                                    newConn.setConditionExpr(conn.getConditionExpr());
                                    newConn.setOrderNum(conn.getOrderNum());
                                    newConnections.add(newConn);
                                }
                            }
                            saveRequest.setConnections(newConnections);
                            
                            // 保存新工作流
                            workflowDefinitionService.save(saveRequest, userId);
                        } catch (Exception e) {
                            // 如果复制某个工作流失败，记录日志但继续复制其他工作流
                            LogUtils.error("复制工作流失败: workflowId=" + sourceWorkflow.getWorkflowId() + 
                                    ", moduleId=" + sourceModule.getId() + ", error=" + e.getMessage(), e);
                        }
                    }
                }
            }
        } else {
            // 如果没有原模块，创建默认模块
            MetadataModule defaultModule = new MetadataModule();
            String defaultModuleId = IDGenerator.nextStr();
            defaultModule.setId(defaultModuleId);
            defaultModule.setProjectId(workspace.getProjectId());
            defaultModule.setTypeId(workspace.getWorkspaceId());
            defaultModule.setParentId("ROOT");
            defaultModule.setName("默认模块");
            defaultModule.setModuleType("WORKFLOW");
            defaultModule.setPos(0L);
            defaultModule.setCreateTime(currentTime);
            defaultModule.setUpdateTime(currentTime);
            defaultModule.setDeletedTime(null);
            metadataModuleMapper.insert(defaultModule);

            // ⭐ 在默认模块下创建"工作流同步"子模块
            MetadataModule syncModule = new MetadataModule();
            syncModule.setId(IDGenerator.nextStr());
            syncModule.setProjectId(workspace.getProjectId());
            syncModule.setTypeId(workspace.getWorkspaceId());
            syncModule.setParentId(defaultModuleId); // 父模块为默认模块
            syncModule.setName("工作流同步");
            syncModule.setModuleType("WORKFLOW");
            syncModule.setPos(0L);
            syncModule.setCreateTime(currentTime);
            syncModule.setUpdateTime(currentTime);
            syncModule.setDeletedTime(null);
            metadataModuleMapper.insert(syncModule);
        }

        return convertToDTO(workspace);
    }

    /**
     * 删除工作空间（软删除）
     * 同时软删除该空间下的所有模块和用例
     */
    public void delete(String workspaceId, String userId) {
        WorkflowWorkspace workspace = workflowWorkspaceMapper.selectByWorkspaceId(workspaceId);
        if (workspace == null) {
            throw new MSException("工作空间不存在");
        }

        long currentTime = System.currentTimeMillis();
        LocalDateTime deletedTime = LocalDateTime.now();

        // 1. 查询该空间下的所有模块
        List<MetadataModule> modules = metadataModuleMapper.selectByProjectIdAndTypeId(
                workspace.getProjectId(), workspaceId, "WORKFLOW");
        
        if (CollectionUtils.isNotEmpty(modules)) {
            // 2. 软删除所有模块下的工作流用例
            for (MetadataModule module : modules) {
                List<WorkflowDefinition> workflows = workflowDefinitionMapper.selectByModuleId(module.getId());
                if (CollectionUtils.isNotEmpty(workflows)) {
                    for (WorkflowDefinition workflow : workflows) {
                        WorkflowDefinition updateWorkflow = new WorkflowDefinition();
                        updateWorkflow.setWorkflowId(workflow.getWorkflowId());
                        updateWorkflow.setDeletedTime(deletedTime);
                        updateWorkflow.setUpdateTime(currentTime);
                        updateWorkflow.setUpdateUser(userId);
                        workflowDefinitionMapper.updateById(updateWorkflow);
                    }
                }
            }
            
            // 3. 软删除所有模块
            for (MetadataModule module : modules) {
                MetadataModule updateModule = new MetadataModule();
                updateModule.setId(module.getId());
                updateModule.setDeletedTime(currentTime);
                metadataModuleMapper.updateById(updateModule);
            }
        }

        // 4. 软删除工作空间
        workspace.setDeletedBy(userId);
        workspace.setDeletedTime(currentTime);
        workspace.setUpdateTime(currentTime);
        workspace.setUpdateUser(userId);
        workflowWorkspaceMapper.updateById(workspace);

        // 清除缓存
        workflowWorkspaceCache.evictByProjectId(workspace.getProjectId());
    }

    /**
     * 批量加载统计信息（性能优化：避免 N+1 查询）
     */
    private void loadStatisticsBatch(List<WorkflowWorkspaceDTO> dtoList, List<WorkflowWorkspace> workspaces, String projectId) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return;
        }

        // 1. 批量查询所有空间的模块
        List<String> workspaceIds = dtoList.stream()
                .map(WorkflowWorkspaceDTO::getWorkspaceId)
                .collect(Collectors.toList());
        
        List<MetadataModule> allModules = metadataModuleMapper.selectByProjectIdAndTypeIds(
                projectId, workspaceIds, "WORKFLOW");
        
        // 按 workspaceId 分组模块
        Map<String, List<MetadataModule>> modulesByWorkspace = allModules.stream()
                .collect(Collectors.groupingBy(MetadataModule::getTypeId));
        
        // 2. 批量查询所有模块的工作流
        List<String> allModuleIds = allModules.stream()
                .map(MetadataModule::getId)
                .collect(Collectors.toList());
        
        Map<String, List<WorkflowDefinition>> workflowsByModule = new HashMap<>();
        if (CollectionUtils.isNotEmpty(allModuleIds)) {
            List<WorkflowDefinition> allWorkflows = workflowDefinitionMapper.selectByModuleIds(allModuleIds);
            workflowsByModule = allWorkflows.stream()
                    .collect(Collectors.groupingBy(WorkflowDefinition::getModuleId));
        }
        
        // 3. 收集所有工作流ID，批量查询执行记录
        List<String> allWorkflowIds = new ArrayList<>();
        Map<String, List<String>> workflowIdsByWorkspace = new HashMap<>();
        
        for (WorkflowWorkspaceDTO dto : dtoList) {
            List<String> workspaceWorkflowIds = new ArrayList<>();
            List<MetadataModule> workspaceModules = modulesByWorkspace.getOrDefault(dto.getWorkspaceId(), new ArrayList<>());
            
            for (MetadataModule module : workspaceModules) {
                List<WorkflowDefinition> moduleWorkflows = workflowsByModule.getOrDefault(module.getId(), new ArrayList<>());
                for (WorkflowDefinition workflow : moduleWorkflows) {
                    workspaceWorkflowIds.add(workflow.getWorkflowId());
                }
            }
            
            workflowIdsByWorkspace.put(dto.getWorkspaceId(), workspaceWorkflowIds);
            allWorkflowIds.addAll(workspaceWorkflowIds);
        }
        
        // 批量查询所有执行记录
        Map<String, List<WorkflowRun>> runsByWorkflow = new HashMap<>();
        if (CollectionUtils.isNotEmpty(allWorkflowIds)) {
            List<WorkflowRun> allRuns = workflowRunMapper.selectByWorkflowIds(allWorkflowIds);
            runsByWorkflow = allRuns.stream()
                    .collect(Collectors.groupingBy(WorkflowRun::getWorkflowId));
        }
        
        // 4. 为每个空间计算统计信息
        for (WorkflowWorkspaceDTO dto : dtoList) {
            String workspaceId = dto.getWorkspaceId();
            List<MetadataModule> workspaceModules = modulesByWorkspace.getOrDefault(workspaceId, new ArrayList<>());
            
            // 模块数
            dto.setModuleCount(workspaceModules.size());
            
            // 测试用例数
            int testCaseCount = 0;
            for (MetadataModule module : workspaceModules) {
                List<WorkflowDefinition> moduleWorkflows = workflowsByModule.getOrDefault(module.getId(), new ArrayList<>());
                testCaseCount += moduleWorkflows.size();
            }
            dto.setTestCaseCount(testCaseCount);
            
            // 成员数暂时固定为 0：当前只存在项目级 user_role_relation，
            // 但 workflow_workspace 尚未落地独立成员关系模型，不能错误复用项目成员数。
            dto.setMemberCount(0);
            
            // 通过率、状态、最后运行时间
            List<String> workspaceWorkflowIds = workflowIdsByWorkspace.getOrDefault(workspaceId, new ArrayList<>());
            
            if (CollectionUtils.isEmpty(workspaceWorkflowIds)) {
                dto.setPassRate(0.0);
                dto.setStatus("not-run");
                dto.setLastRun("从未运行");
            } else {
                // 收集该空间下所有工作流的执行记录
                List<WorkflowRun> allRuns = new ArrayList<>();
                for (String workflowId : workspaceWorkflowIds) {
                    List<WorkflowRun> runs = runsByWorkflow.getOrDefault(workflowId, new ArrayList<>());
                    allRuns.addAll(runs);
                }
                
                if (CollectionUtils.isEmpty(allRuns)) {
                    dto.setPassRate(0.0);
                    dto.setStatus("not-run");
                    dto.setLastRun("从未运行");
                } else {
                    // 计算通过率
                    int totalPassedNodes = 0;
                    int totalExecutedNodes = 0;
                    for (WorkflowRun run : allRuns) {
                        int passedCount = run.getPassedCount() != null ? run.getPassedCount() : 0;
                        int failedCount = run.getFailedCount() != null ? run.getFailedCount() : 0;
                        totalPassedNodes += passedCount;
                        totalExecutedNodes += (passedCount + failedCount);
                    }
                    
                    double passRate = totalExecutedNodes > 0 ? (double) totalPassedNodes / totalExecutedNodes * 100 : 0.0;
                    dto.setPassRate(Math.round(passRate * 100.0) / 100.0);
                    
                    // 获取最后一次运行的记录
                    WorkflowRun latestRun = allRuns.stream()
                            .sorted((r1, r2) -> {
                                Long time1 = r1.getCreateTime() != null ? r1.getCreateTime() : 0L;
                                Long time2 = r2.getCreateTime() != null ? r2.getCreateTime() : 0L;
                                return time2.compareTo(time1);
                            })
                            .findFirst()
                            .orElse(null);
                    
                    if (latestRun != null) {
                        String runStatus = latestRun.getStatus();
                        if ("SUCCESS".equals(runStatus) || "SUCCEED".equals(runStatus)) {
                            dto.setStatus("running");
                        } else if ("FAILED".equals(runStatus) || "FAIL".equals(runStatus)) {
                            dto.setStatus("failed");
                        } else {
                            dto.setStatus("not-run");
                        }
                        
                        Long lastRunTime = latestRun.getStartTime() != null ? latestRun.getStartTime() : latestRun.getCreateTime();
                        if (lastRunTime != null) {
                            long currentTime = System.currentTimeMillis();
                            long diff = currentTime - lastRunTime;
                            long hours = diff / (1000 * 60 * 60);
                            long days = diff / (1000 * 60 * 60 * 24);
                            
                            if (days > 0) {
                                dto.setLastRun(days + "天前");
                            } else if (hours > 0) {
                                dto.setLastRun(hours + "小时前");
                            } else {
                                long minutes = diff / (1000 * 60);
                                if (minutes > 0) {
                                    dto.setLastRun(minutes + "分钟前");
                                } else {
                                    dto.setLastRun("刚刚");
                                }
                            }
                        } else {
                            dto.setLastRun("从未运行");
                        }
                    } else {
                        dto.setStatus("not-run");
                        dto.setLastRun("从未运行");
                    }
                }
            }
        }
    }

    /**
     * 加载统计信息（单个空间，用于详情接口）
     */
    private void loadStatistics(WorkflowWorkspaceDTO dto, WorkflowWorkspace workspace) {
        String workspaceId = workspace.getWorkspaceId();
        String projectId = workspace.getProjectId();

        // 1. 查询模块数：根据 project_id 和 type_id (workspaceId) 查询 WORKFLOW 类型的模块
        List<MetadataModule> modules = metadataModuleMapper.selectByProjectIdAndTypeId(
                projectId, workspaceId, "WORKFLOW");
        dto.setModuleCount(modules != null ? modules.size() : 0);

        // 2. 查询测试用例数：通过模块ID查询工作流定义
        int testCaseCount = 0;
        if (CollectionUtils.isNotEmpty(modules)) {
            List<String> moduleIds = modules.stream().map(MetadataModule::getId).collect(Collectors.toList());
            for (String moduleId : moduleIds) {
                List<WorkflowDefinition> workflows = workflowDefinitionMapper.selectByModuleId(moduleId);
                if (CollectionUtils.isNotEmpty(workflows)) {
                    testCaseCount += workflows.size();
                }
            }
        }
        dto.setTestCaseCount(testCaseCount);

        // 3. 成员数暂时固定为 0：当前只存在项目级 user_role_relation，
        // 但 workflow_workspace 尚未落地独立成员关系模型，不能错误复用项目成员数。
        dto.setMemberCount(0);

        // 4. 通过率、状态、最后运行时间
        // 获取空间下所有工作流ID
        List<String> workflowIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(modules)) {
            List<String> moduleIds = modules.stream().map(MetadataModule::getId).collect(Collectors.toList());
            for (String moduleId : moduleIds) {
                List<WorkflowDefinition> workflows = workflowDefinitionMapper.selectByModuleId(moduleId);
                if (CollectionUtils.isNotEmpty(workflows)) {
                    workflowIds.addAll(workflows.stream().map(WorkflowDefinition::getWorkflowId).collect(Collectors.toList()));
                }
            }
        }

        if (CollectionUtils.isEmpty(workflowIds)) {
            // 没有工作流，设置默认值
            dto.setPassRate(0.0);
            dto.setStatus("not-run");
            dto.setLastRun("从未运行");
        } else {
            // 查询所有工作流的执行记录（排除DEBUG类型）
            List<WorkflowRun> allRuns = new ArrayList<>();
            for (String workflowId : workflowIds) {
                List<WorkflowRun> runs = workflowRunMapper.selectByWorkflowId(workflowId);
                if (CollectionUtils.isNotEmpty(runs)) {
                    allRuns.addAll(runs);
                }
            }

            if (CollectionUtils.isEmpty(allRuns)) {
                // 没有执行记录
        dto.setPassRate(0.0);
        dto.setStatus("not-run");
        dto.setLastRun("从未运行");
            } else {
                // 计算通过率：执行的用例中的通过节点数/空间下面执行过的总节点数
                // 总执行过的节点数 = passedCount + failedCount（只统计执行过的节点，不包括未执行的）
                int totalPassedNodes = 0;
                int totalExecutedNodes = 0;
                for (WorkflowRun run : allRuns) {
                    int passedCount = run.getPassedCount() != null ? run.getPassedCount() : 0;
                    int failedCount = run.getFailedCount() != null ? run.getFailedCount() : 0;
                    totalPassedNodes += passedCount;
                    totalExecutedNodes += (passedCount + failedCount); // 只统计执行过的节点
                }
                
                double passRate = totalExecutedNodes > 0 ? (double) totalPassedNodes / totalExecutedNodes * 100 : 0.0;
                dto.setPassRate(Math.round(passRate * 100.0) / 100.0); // 保留两位小数

                // 获取最后一次运行的记录（按创建时间倒序）
                WorkflowRun latestRun = allRuns.stream()
                    .sorted((r1, r2) -> {
                        Long time1 = r1.getCreateTime() != null ? r1.getCreateTime() : 0L;
                        Long time2 = r2.getCreateTime() != null ? r2.getCreateTime() : 0L;
                        return time2.compareTo(time1);
                    })
                    .findFirst()
                    .orElse(null);

                if (latestRun != null) {
                    // 设置状态：根据最后一次运行的状态
                    String runStatus = latestRun.getStatus();
                    if ("SUCCESS".equals(runStatus) || "SUCCEED".equals(runStatus)) {
                        dto.setStatus("running"); // 运行正常
                    } else if ("FAILED".equals(runStatus) || "FAIL".equals(runStatus)) {
                        dto.setStatus("failed"); // 有失败
                    } else {
                        dto.setStatus("not-run");
                    }

                    // 设置最后运行时间
                    Long lastRunTime = latestRun.getStartTime() != null ? latestRun.getStartTime() : latestRun.getCreateTime();
                    if (lastRunTime != null) {
                        // 格式化为相对时间（如：2小时前）
                        long currentTime = System.currentTimeMillis();
                        long diff = currentTime - lastRunTime;
                        long hours = diff / (1000 * 60 * 60);
                        long days = diff / (1000 * 60 * 60 * 24);
                        
                        if (days > 0) {
                            dto.setLastRun(days + "天前");
                        } else if (hours > 0) {
                            dto.setLastRun(hours + "小时前");
                        } else {
                            long minutes = diff / (1000 * 60);
                            if (minutes > 0) {
                                dto.setLastRun(minutes + "分钟前");
                            } else {
                                dto.setLastRun("刚刚");
                            }
                        }
                    } else {
                        dto.setLastRun("从未运行");
                    }
                } else {
                    dto.setStatus("not-run");
                    dto.setLastRun("从未运行");
                }
            }
        }
    }

    /**
     * 转换为 DTO
     */
    private WorkflowWorkspaceDTO convertToDTO(WorkflowWorkspace workspace) {
        WorkflowWorkspaceDTO dto = new WorkflowWorkspaceDTO();
        dto.setWorkspaceId(workspace.getWorkspaceId());
        dto.setWorkspaceName(workspace.getWorkspaceName());
        dto.setProjectId(workspace.getProjectId());
        dto.setOwner(workspace.getOwner());
        dto.setDescription(workspace.getDescription());
        dto.setIcon(workspace.getIcon());
        dto.setIconColor(workspace.getIconColor());
        dto.setVisibility(workspace.getVisibility());
        dto.setGlobalVars(workspace.getGlobalVars());
        dto.setCreateTime(workspace.getCreateTime());
        dto.setUpdateTime(workspace.getUpdateTime());

        // 统计字段在 loadStatistics 方法中设置

        return dto;
    }

    /**
     * 根据节点类型获取节点颜色
     * 颜色定义与前端 NODE_META_REGISTRY 保持一致
     */
    private String getNodeColor(String nodeType) {
        if (StringUtils.isBlank(nodeType)) {
            return "#10B981"; // 默认绿色
        }
        
        String lowerType = nodeType.toLowerCase();
        
        // 根据节点类型返回对应的颜色
        switch (lowerType) {
            case "http_request":
            case "http":
            case "api":
                return "#3B82F6"; // 蓝色
            case "mysql":
            case "sql":
                return "#8B5CF6"; // 紫色
            case "dubbo":
                return "#F59E0B"; // 橙色
            case "condition":
            case "if":
                return "#F59E0B"; // 橙色
            case "loop":
            case "foreach":
                return "#EC4899"; // 粉色
            case "script":
            case "python":
                return "#10B981"; // 绿色
            case "assertion":
                return "#6366F1"; // 靛蓝色
            case "variable_extractor":
                return "#10B981"; // 绿色
            case "log_message":
            case "log":
                return "#6366F1"; // 靛蓝色
            case "rocketmq":
            case "mq":
                return "#8B5CF6"; // 紫色
            case "sub_workflow":
                return "#10B981"; // 绿色
            case "redis":
                return "#DC2626"; // 红色
            case "mongodb":
                return "#10B981"; // 绿色
            case "oss":
                return "#3B82F6"; // 蓝色
            case "xxl_job":
            case "xxljob":
                return "#F59E0B"; // 橙色
            default:
                return "#10B981"; // 默认绿色
        }
    }
}
