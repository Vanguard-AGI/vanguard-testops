package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流步骤 Mapper 接口
 */
@Mapper
public interface WorkflowStepMapper extends BaseMapper<WorkflowStep> {

    /**
     * 根据工作流ID查询所有步骤（排除已删除，按顺序排序）
     * 注意：使用 XML 中的 resultMap 来正确映射 step_config 字段
     */
    List<WorkflowStep> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据工作流ID列表批量查询步骤（排除已删除）
     * 用于列表页批量统计步骤数，避免 N+1 查询
     */
    List<WorkflowStep> selectByWorkflowIds(@Param("workflowIds") List<String> workflowIds);

    /**
     * 根据工作流ID删除所有步骤（物理删除，用于更新时先删后插）
     */
    @Delete("DELETE FROM workflow_step WHERE workflow_id = #{workflowId}")
    int deleteByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 批量插入步骤
     */
    int batchInsert(@Param("list") List<WorkflowStep> steps);
}

