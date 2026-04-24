package io.vanguard.testops.metadata.service;

import io.vanguard.testops.metadata.domain.MetadataModule;
import io.vanguard.testops.metadata.dto.MetadataModuleCreateRequest;
import io.vanguard.testops.metadata.dto.MetadataModuleUpdateRequest;
import io.vanguard.testops.metadata.mapper.MetadataDefinitionMapper;
import io.vanguard.testops.metadata.mapper.MetadataModuleMapper;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.sdk.constants.ModuleConstants;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据模块服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MetadataModuleService {

    @Resource
    private MetadataModuleMapper metadataModuleMapper;

    @Resource
    private MetadataDefinitionMapper metadataDefinitionMapper;

    /**
     * 创建模块
     */
    public String create(MetadataModuleCreateRequest request, String userId) {
        MetadataModule module = new MetadataModule();
        module.setId(IDGenerator.nextStr());
        module.setProjectId(request.getProjectId());
        module.setParentId(StringUtils.isNotBlank(request.getParentId()) ? request.getParentId() : "ROOT");
        module.setName(request.getName());
        module.setModuleType(StringUtils.isNotBlank(request.getModuleType()) ? request.getModuleType() : "MIXED");
        // 设置 typeId（当 moduleType='WORKFLOW' 时，存储 workspaceId）
        if (StringUtils.isNotBlank(request.getTypeId())) {
            module.setTypeId(request.getTypeId());
        }
        
        // 校验数据合法性
        checkDataValidity(module);
        
        long currentTime = System.currentTimeMillis();
        module.setCreateTime(currentTime);
        module.setUpdateTime(currentTime);
        module.setPos(getNextOrder(request.getParentId()));
        // deleted_time 不设置，默认为 NULL（表示未删除）
        
        metadataModuleMapper.insert(module);
        
        return module.getId();
    }

    /**
     * 更新模块
     */
    public void update(MetadataModuleUpdateRequest request, String userId) {
        MetadataModule existing = checkModuleExist(request.getId());
        
        MetadataModule module = new MetadataModule();
        module.setId(request.getId());
        module.setName(request.getName());
        
        // 校验数据合法性
        // checkDataValidity(module);
        
        module.setUpdateTime(System.currentTimeMillis());
        
        metadataModuleMapper.updateById(module);
    }

    /**
     * 删除模块(物理删除，设置 deleted_time)
     */
    public void delete(String id, String userId) {
        MetadataModule module = checkModuleExist(id);
        
        MetadataModule updateModule = new MetadataModule();
        updateModule.setId(id);
        updateModule.setDeletedTime(System.currentTimeMillis());
        
        metadataModuleMapper.updateById(updateModule);
    }

    /**
     * 获取模块树
     * @param projectId 项目ID
     * @param typeId 类型ID（可选，当 moduleType='WORKFLOW' 时传入 workspaceId）
     * @param moduleType 模块类型（可选，如 'WORKFLOW'）
     */
    public List<BaseTreeNode> getTree(String projectId, String typeId, String moduleType) {
        List<MetadataModule> modules;
        
        // 如果提供了 typeId 和 moduleType，则使用精确查询
        if (StringUtils.isNotBlank(typeId) && StringUtils.isNotBlank(moduleType)) {
            modules = metadataModuleMapper.selectByProjectIdAndTypeId(projectId, typeId, moduleType);
        } else if (StringUtils.isNotBlank(moduleType)) {
            // 如果只提供了 moduleType，则按模块类型查询
            modules = metadataModuleMapper.selectByProjectIdAndModuleType(projectId, moduleType);
        } else {
            // 否则查询项目下的所有模块
            modules = metadataModuleMapper.selectByProjectId(projectId);
        }
        
        if (modules == null || modules.isEmpty()) {
            return new ArrayList<>();
        }

        // 将 MetadataModule 转换为 BaseTreeNode
        List<BaseTreeNode> traverseList = new ArrayList<>();
        for (MetadataModule module : modules) {
            BaseTreeNode node = new BaseTreeNode();
            node.setId(module.getId());
            node.setName(module.getName());
            node.setType(module.getModuleType());
            // 将 "ROOT" 转换为 "NONE"（ROOT_NODE_PARENT_ID）
            String parentId = module.getParentId();
            if (StringUtils.equalsIgnoreCase(parentId, "ROOT")) {
                parentId = ModuleConstants.ROOT_NODE_PARENT_ID;
            }
            node.setParentId(parentId);
            node.setProjectId(module.getProjectId());
            traverseList.add(node);
        }

        // 查询每个模块下的定义数量
        List<ModuleCountDTO> moduleCountDTOList = metadataDefinitionMapper.countByModuleId(projectId);
        Map<String, Integer> resourceCountMap = moduleCountDTOList.stream()
                .collect(Collectors.groupingBy(
                        ModuleCountDTO::getModuleId,
                        Collectors.summingInt(ModuleCountDTO::getDataCount)
                ));

        // 构建树结构
        List<BaseTreeNode> tree = buildTree(traverseList);

        // 为每个节点赋值资源数量（包含子节点）
        sumModuleResourceCount(tree, resourceCountMap);

        return tree;
    }

    /**
     * 构建树结构
     */
    private List<BaseTreeNode> buildTree(List<BaseTreeNode> traverseList) {
        List<BaseTreeNode> baseTreeNodeList = new ArrayList<>();
        Map<String, BaseTreeNode> baseTreeNodeMap = new HashMap<>();

        // 根节点预先处理（parentId 为 "NONE" 或 "ROOT"）
        List<BaseTreeNode> rootNodes = new ArrayList<>();
        for (BaseTreeNode treeNode : traverseList) {
            if (StringUtils.equalsIgnoreCase(treeNode.getParentId(), ModuleConstants.ROOT_NODE_PARENT_ID)
                    || StringUtils.equalsIgnoreCase(treeNode.getParentId(), "ROOT")) {
                rootNodes.add(treeNode);
            }
        }

        for (BaseTreeNode rootNode : rootNodes) {
            BaseTreeNode node = new BaseTreeNode(rootNode.getId(), rootNode.getName(), rootNode.getType(), ModuleConstants.ROOT_NODE_PARENT_ID);
            node.setProjectId(rootNode.getProjectId());
            node.genModulePath(null);
            baseTreeNodeList.add(node);
            baseTreeNodeMap.put(node.getId(), node);
        }

        // 移除已处理的根节点
        traverseList.removeAll(rootNodes);

        // 循环处理子节点
        int lastSize = 0;
        while (!traverseList.isEmpty() && traverseList.size() != lastSize) {
            lastSize = traverseList.size();
            List<BaseTreeNode> notMatchedList = new ArrayList<>();

            for (BaseTreeNode treeNode : traverseList) {
                String parentId = treeNode.getParentId();
                // 将 "ROOT" 转换为 "NONE"
                if (StringUtils.equalsIgnoreCase(parentId, "ROOT")) {
                    parentId = ModuleConstants.ROOT_NODE_PARENT_ID;
                }

                if (!baseTreeNodeMap.containsKey(parentId)
                        && !StringUtils.equalsIgnoreCase(parentId, ModuleConstants.ROOT_NODE_PARENT_ID)) {
                    notMatchedList.add(treeNode);
                    continue;
                }

                BaseTreeNode node = new BaseTreeNode(treeNode.getId(), treeNode.getName(), treeNode.getType(), parentId);
                node.setProjectId(treeNode.getProjectId());
                node.genModulePath(baseTreeNodeMap.get(parentId));
                baseTreeNodeMap.put(node.getId(), node);

                if (StringUtils.equalsIgnoreCase(parentId, ModuleConstants.ROOT_NODE_PARENT_ID)) {
                    baseTreeNodeList.add(node);
                } else if (baseTreeNodeMap.containsKey(parentId)) {
                    baseTreeNodeMap.get(parentId).addChild(node);
                }
            }
            traverseList = notMatchedList;
        }

        return baseTreeNodeList;
    }

    /**
     * 校验数据合法性
     */
    private void checkDataValidity(MetadataModule module) {
        // 简化实现：跳过校验
    }

    /**
     * 获取下一个排序号
     */
    private Long getNextOrder(String parentId) {
        return 5000L;
    }

    /**
     * 检查模块是否存在
     * 注意：selectByIdWithTimestamp 已经过滤了已删除的记录（deleted_time IS NULL）
     */
    private MetadataModule checkModuleExist(String moduleId) {
        MetadataModule module = metadataModuleMapper.selectByIdWithTimestamp(moduleId);
        if (module == null) {
            throw new MSException("模块不存在");
        }
        return module;
    }

    /**
     * 通过深度遍历的方式，在为节点赋值资源统计数量的同时，同步计算其子节点的资源数量，并添加到父节点上
     */
    private void sumModuleResourceCount(List<BaseTreeNode> baseTreeNodeList, Map<String, Integer> resourceCountMap) {
        for (BaseTreeNode node : baseTreeNodeList) {
            // 赋值子节点的资源数量
            sumModuleResourceCount(node.getChildren(), resourceCountMap);
            // 当前节点的资源数量（包含子节点）
            long childResourceCount = 0;
            for (BaseTreeNode childNode : node.getChildren()) {
                childResourceCount += childNode.getCount();
            }

            if (resourceCountMap.containsKey(node.getId())) {
                node.setCount(childResourceCount + resourceCountMap.get(node.getId()));
            } else {
                node.setCount(childResourceCount);
            }
        }
    }

    /**
     * 初始化根目录：创建 SQL、API、DUBBO、ROCKETMQ、SCRIPT、FILE 的根目录
     * 对于 API 和 DUBBO 场景，自动创建"测试数据"子模块
     */
    public void initRootModules(String projectId, String userId) {
        List<MetadataModule> existingModules = metadataModuleMapper.selectByProjectId(projectId);
        Map<String, MetadataModule> moduleMap = new HashMap<>();
        Map<String, MetadataModule> allModulesMap = new HashMap<>();
        for (MetadataModule module : existingModules) {
            allModulesMap.put(module.getId(), module);
            if ("ROOT".equals(module.getParentId())) {
                moduleMap.put(module.getModuleType(), module);
            }
        }

        String[] moduleTypes = {"API", "SQL", "DUBBO", "ROCKETMQ", "SCRIPT", "FILE"};
        String[] moduleNames = {"HTTP接口", "SQL操作", "DUBBO服务", "ROCKETMQ消息", "造数工厂", "文件上传"};
        
        long currentTime = System.currentTimeMillis();
        long basePos = 1000L;

        for (int i = 0; i < moduleTypes.length; i++) {
            String moduleType = moduleTypes[i];
            String moduleName = moduleNames[i];
            
            MetadataModule rootModule;
            if (!moduleMap.containsKey(moduleType)) {
                rootModule = new MetadataModule();
                rootModule.setId(IDGenerator.nextStr());
                rootModule.setProjectId(projectId);
                rootModule.setParentId("ROOT");
                rootModule.setName(moduleName);
                rootModule.setModuleType(moduleType);
                rootModule.setPos(basePos + i * 1000L);
                rootModule.setCreateTime(currentTime);
                rootModule.setUpdateTime(currentTime);
                
                metadataModuleMapper.insert(rootModule);
                moduleMap.put(moduleType, rootModule);
                allModulesMap.put(rootModule.getId(), rootModule);
            } else {
                rootModule = moduleMap.get(moduleType);
            }
            
            if ("API".equals(moduleType) || "DUBBO".equals(moduleType)) {
                initTestDataModule(projectId, rootModule.getId(), moduleType, existingModules, currentTime);
            }
        }
    }

    /**
     * 初始化"测试数据"子模块
     * @param projectId 项目ID
     * @param parentId 父模块ID
     * @param moduleType 模块类型
     * @param existingModules 已存在的模块列表（用于初步检查）
     * @param currentTime 当前时间戳
     */
    private void initTestDataModule(String projectId, String parentId, String moduleType, 
                                    List<MetadataModule> existingModules, long currentTime) {
        String testDataModuleName = "测试数据";
        
        boolean exists = false;
        for (MetadataModule module : existingModules) {
            if (testDataModuleName.equals(module.getName()) && parentId.equals(module.getParentId())) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            List<MetadataModule> allModules = metadataModuleMapper.selectByProjectId(projectId);
            for (MetadataModule module : allModules) {
                if (testDataModuleName.equals(module.getName()) && parentId.equals(module.getParentId())) {
                    exists = true;
                    break;
                }
            }
        }
        
        if (!exists) {
            MetadataModule testDataModule = new MetadataModule();
            testDataModule.setId(IDGenerator.nextStr());
            testDataModule.setProjectId(projectId);
            testDataModule.setParentId(parentId);
            testDataModule.setName(testDataModuleName);
            testDataModule.setModuleType(moduleType);
            testDataModule.setPos(100L);
            testDataModule.setCreateTime(currentTime);
            testDataModule.setUpdateTime(currentTime);
            
            metadataModuleMapper.insert(testDataModule);
        }
    }
}
