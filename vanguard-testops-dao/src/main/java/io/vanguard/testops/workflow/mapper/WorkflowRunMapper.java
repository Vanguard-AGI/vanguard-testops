package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流运行记录 Mapper
 */
public interface WorkflowRunMapper extends BaseMapper<WorkflowRun> {
    
    /**
     * 根据工作流ID查询运行记录列表
     */
    List<WorkflowRun> selectByWorkflowId(@Param("workflowId") String workflowId);
    
    /**
     * 根据工作流ID、项目ID和触发用户查询运行记录列表（用于历史记录查询）
     */
    List<WorkflowRun> selectByWorkflowIdAndFilters(
        @Param("workflowId") String workflowId,
        @Param("projectId") String projectId,
        @Param("triggerUser") String triggerUser
    );
    
    /**
     * 根据项目ID查询运行记录列表
     */
    List<WorkflowRun> selectByProjectId(@Param("projectId") String projectId);
    
    /**
     * 根据运行ID查询（包含软删除的数据）
     */
    WorkflowRun selectByRunId(@Param("runId") String runId);
    
    /**
     * 根据工作流ID查询最新的一条运行记录（排除调试记录）
     */
    WorkflowRun selectLatestByWorkflowId(@Param("workflowId") String workflowId);
    
    /**
     * 批量查询运行记录：根据工作流ID列表查询运行记录列表（排除调试记录）
     * 用于性能优化，避免 N+1 查询
     */
    List<WorkflowRun> selectByWorkflowIds(@Param("workflowIds") List<String> workflowIds);
}

