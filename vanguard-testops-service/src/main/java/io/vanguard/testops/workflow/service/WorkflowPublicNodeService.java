package io.vanguard.testops.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.workflow.domain.WorkflowPublicNode;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodeDTO;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodePageRequest;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodeSaveRequest;
import io.vanguard.testops.workflow.mapper.WorkflowPublicNodeMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流公共节点服务
 */
@Service
public class WorkflowPublicNodeService {

    @Resource
    private WorkflowPublicNodeMapper publicNodeMapper;

    /**
     * 保存公共节点（创建或更新）
     * @param request 保存请求
     * @param userId 当前用户ID
     * @return 保存后的节点DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowPublicNodeDTO savePublicNode(WorkflowPublicNodeSaveRequest request, String userId) {
        long currentTime = System.currentTimeMillis();
        WorkflowPublicNode node;

        if (StringUtils.isNotBlank(request.getId())) {
            // 更新
            node = publicNodeMapper.selectById(request.getId());
            if (node == null) {
                throw new RuntimeException("节点不存在: " + request.getId());
            }
            if (!request.getProjectId().equals(node.getProjectId())) {
                throw new RuntimeException("无权操作该节点，节点不属于当前项目");
            }
            if (node.getDeletedTime() != null) {
                throw new RuntimeException("节点已被删除，无法更新");
            }
            node.setName(request.getName());
            node.setDescription(request.getDescription());
            node.setType(request.getType());
            node.setCategory(request.getCategory());
            node.setConfig(request.getConfig());
            node.setUpdateTime(currentTime);
            node.setUpdateUser(userId);
            publicNodeMapper.updateById(node);
            LogUtils.info("更新公共节点成功: id={}, projectId={}, userId={}", request.getId(), request.getProjectId(), userId);
        } else {
            // 创建
            node = new WorkflowPublicNode();
            node.setId(IDGenerator.nextStr());
            node.setProjectId(request.getProjectId());
            node.setName(request.getName());
            node.setDescription(request.getDescription());
            node.setType(request.getType());
            node.setCategory(request.getCategory());
            node.setConfig(request.getConfig());
            node.setCreateTime(currentTime);
            node.setUpdateTime(currentTime);
            node.setCreateUser(userId);
            node.setUpdateUser(userId);
            node.setDeletedTime(null);
            node.setDeletedBy(null);
            publicNodeMapper.insert(node);
            LogUtils.info("创建公共节点成功: id={}, projectId={}, userId={}", node.getId(), request.getProjectId(), userId);
        }

        return convertToDTO(node);
    }

    /**
     * 分页查询公共节点列表
     * @param request 查询请求
     * @return 分页结果
     */
    public IPage<WorkflowPublicNodeDTO> getPublicNodePage(WorkflowPublicNodePageRequest request) {
        Page<WorkflowPublicNode> page = new Page<>(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<WorkflowPublicNode> queryWrapper = new LambdaQueryWrapper<>();

        // 项目：支持多选 projectIds，或单选 projectId
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            queryWrapper.in(WorkflowPublicNode::getProjectId, request.getProjectIds());
        } else if (StringUtils.isNotBlank(request.getProjectId())) {
            queryWrapper.eq(WorkflowPublicNode::getProjectId, request.getProjectId());
        } else {
            return new Page<>(request.getCurrent(), request.getPageSize(), 0);
        }
        
        // 未删除
        queryWrapper.isNull(WorkflowPublicNode::getDeletedTime);
        
        // 分类过滤
        if (StringUtils.isNotBlank(request.getCategory())) {
            queryWrapper.eq(WorkflowPublicNode::getCategory, request.getCategory());
        }
        
        // 关键词搜索（节点名称或描述）
        if (StringUtils.isNotBlank(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(WorkflowPublicNode::getName, request.getKeyword())
                .or()
                .like(WorkflowPublicNode::getDescription, request.getKeyword())
            );
        }
        
        // 按创建时间倒序
        queryWrapper.orderByDesc(WorkflowPublicNode::getCreateTime);
        
        IPage<WorkflowPublicNode> nodePage = publicNodeMapper.selectPage(page, queryWrapper);
        
        // 转换为DTO
        IPage<WorkflowPublicNodeDTO> dtoPage = new Page<>(nodePage.getCurrent(), nodePage.getSize(), nodePage.getTotal());
        List<WorkflowPublicNodeDTO> dtoList = nodePage.getRecords().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    /**
     * 查询公共节点列表（不分页，用于前端直接展示）
     * @param projectId 项目ID
     * @param category 分类（可选）
     * @return 节点列表
     */
    public List<WorkflowPublicNodeDTO> getPublicNodeList(String projectId, String category) {
        LambdaQueryWrapper<WorkflowPublicNode> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.eq(WorkflowPublicNode::getProjectId, projectId);
        queryWrapper.isNull(WorkflowPublicNode::getDeletedTime);
        
        if (StringUtils.isNotBlank(category)) {
            queryWrapper.eq(WorkflowPublicNode::getCategory, category);
        }
        
        queryWrapper.orderByDesc(WorkflowPublicNode::getCreateTime);
        
        List<WorkflowPublicNode> nodes = publicNodeMapper.selectList(queryWrapper);
        return nodes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 删除公共节点（软删除）
     * @param id 节点ID
     * @param projectId 项目ID（用于验证权限）
     * @param userId 当前用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePublicNode(String id, String projectId, String userId) {
        WorkflowPublicNode node = publicNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("节点不存在: " + id);
        }
        if (!projectId.equals(node.getProjectId())) {
            throw new RuntimeException("无权删除该节点，节点不属于当前项目");
        }
        if (node.getDeletedTime() != null) {
            throw new RuntimeException("节点已被删除");
        }
        
        node.setDeletedTime(java.time.LocalDateTime.now());
        node.setDeletedBy(userId);
        publicNodeMapper.updateById(node);
        
        LogUtils.info("删除公共节点成功: id={}, projectId={}, userId={}", id, projectId, userId);
    }

    /**
     * 转换为 DTO
     */
    private WorkflowPublicNodeDTO convertToDTO(WorkflowPublicNode node) {
        WorkflowPublicNodeDTO dto = new WorkflowPublicNodeDTO();
        dto.setId(node.getId());
        dto.setProjectId(node.getProjectId());
        dto.setName(node.getName());
        dto.setDescription(node.getDescription());
        dto.setType(node.getType());
        dto.setCategory(node.getCategory());
        dto.setConfig(node.getConfig());
        dto.setCreateTime(node.getCreateTime());
        dto.setUpdateTime(node.getUpdateTime());
        dto.setCreateUser(node.getCreateUser());
        dto.setUpdateUser(node.getUpdateUser());
        return dto;
    }
}

