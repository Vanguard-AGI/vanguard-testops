package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowPublicNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流公共节点 Mapper 接口
 */
@Mapper
public interface WorkflowPublicNodeMapper extends BaseMapper<WorkflowPublicNode> {

    /**
     * 根据项目ID查询节点列表（排除已删除）
     * @param projectId 项目ID
     * @return 节点列表
     */
    List<WorkflowPublicNode> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据项目ID和分类查询节点列表（排除已删除）
     * @param projectId 项目ID
     * @param category 节点分类
     * @return 节点列表
     */
    List<WorkflowPublicNode> selectByProjectIdAndCategory(@Param("projectId") String projectId, @Param("category") String category);
}

