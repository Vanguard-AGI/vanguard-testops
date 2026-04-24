package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowRunStep;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流步骤执行明细 Mapper
 */
public interface WorkflowRunStepMapper extends BaseMapper<WorkflowRunStep> {
    
    /**
     * 根据运行ID查询所有步骤执行记录
     */
    List<WorkflowRunStep> selectByRunId(@Param("runId") String runId);
    
    /**
     * 根据步骤ID查询执行记录
     */
    List<WorkflowRunStep> selectByStepId(@Param("stepId") String stepId);
    
    /**
     * 批量插入步骤执行记录
     */
    void batchInsert(@Param("list") List<WorkflowRunStep> steps);
    
    /**
     * 根据运行ID删除所有步骤执行记录
     */
    void deleteByRunId(@Param("runId") String runId);
}

