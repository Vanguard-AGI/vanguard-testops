package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowRunLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流运行日志 Mapper
 */
public interface WorkflowRunLogMapper extends BaseMapper<WorkflowRunLog> {
    
    /**
     * 根据运行ID查询日志列表
     */
    List<WorkflowRunLog> selectByRunId(@Param("runId") String runId);
    
    /**
     * 根据运行步骤ID查询日志列表
     */
    List<WorkflowRunLog> selectByRunStepId(@Param("runStepId") String runStepId);
    
    /**
     * 根据运行ID和运行步骤ID查询日志列表
     */
    List<WorkflowRunLog> selectByRunIdAndRunStepId(@Param("runId") String runId, @Param("runStepId") String runStepId);
    
    /**
     * 批量插入日志
     */
    void batchInsert(@Param("list") List<WorkflowRunLog> logs);
    
    /**
     * 根据运行ID删除所有日志
     */
    void deleteByRunId(@Param("runId") String runId);
}

