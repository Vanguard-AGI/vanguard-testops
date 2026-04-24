package io.vanguard.testops.workflow.mapper;

import io.vanguard.testops.workflow.domain.WorkflowTestReport;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流测试报告 Mapper 扩展接口
 * 包含分页查询和业务查询方法
 */
public interface WorkflowTestReportMapperExt extends WorkflowTestReportMapper {
    
    /**
     * 分页查询测试报告列表（支持多条件筛选）
     * 
     * @param keyword 关键词
     * @param status 状态
     * @param reportType 报告类型
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报告列表
     */
    List<WorkflowTestReport> selectListByConditions(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("reportType") String reportType,
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );
    
    /**
     * 查询报告是否存在
     */
    WorkflowTestReport selectByReportId(@Param("reportId") String reportId);
}

