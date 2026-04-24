package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowWorkspace;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流工作空间 Mapper
 */
public interface WorkflowWorkspaceMapper extends BaseMapper<WorkflowWorkspace> {
    
    /**
     * 根据项目ID查询工作空间列表（支持关键词过滤）
     */
    List<WorkflowWorkspace> selectByProjectId(@Param("projectId") String projectId, @Param("keyword") String keyword);
    
    /**
     * 根据工作空间ID查询（包含软删除的数据）
     */
    WorkflowWorkspace selectByWorkspaceId(@Param("workspaceId") String workspaceId);
}

