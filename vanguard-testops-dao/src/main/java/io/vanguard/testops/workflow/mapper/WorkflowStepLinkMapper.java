package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowStepLink;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流步骤连线 Mapper 接口
 */
@Mapper
public interface WorkflowStepLinkMapper extends BaseMapper<WorkflowStepLink> {

    /**
     * 根据工作流ID查询所有连线（排除已删除，按顺序排序）
     */
    @Select("SELECT * FROM workflow_step_link WHERE workflow_id = #{workflowId} AND deleted_time IS NULL ORDER BY order_num ASC")
    List<WorkflowStepLink> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据工作流ID删除所有连线（物理删除，用于更新时先删后插）
     */
    @Delete("DELETE FROM workflow_step_link WHERE workflow_id = #{workflowId}")
    int deleteByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 批量插入连线
     */
    int batchInsert(@Param("list") List<WorkflowStepLink> links);
}

