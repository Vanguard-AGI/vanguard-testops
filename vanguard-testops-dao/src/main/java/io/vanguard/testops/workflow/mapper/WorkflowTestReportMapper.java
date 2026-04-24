package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowTestReport;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流测试报告 Mapper
 */
public interface WorkflowTestReportMapper extends BaseMapper<WorkflowTestReport> {
    
    /**
     * 根据报告ID查询
     */
    WorkflowTestReport selectByReportId(@Param("reportId") String reportId);
    
    /**
     * 根据项目ID查询报告列表
     */
    List<WorkflowTestReport> selectByProjectId(@Param("projectId") String projectId);
    
    /**
     * 根据报告ID查询关联的所有工作流运行记录ID
     */
    List<String> selectWorkflowRunIdsByReportId(@Param("reportId") String reportId);

    /**
     * 在给定条件下查询已完成报告的平均成功率（success_rate）
     */
    Double selectAvgSuccessRate(@Param("ew") Wrapper<WorkflowTestReport> wrapper);
}

