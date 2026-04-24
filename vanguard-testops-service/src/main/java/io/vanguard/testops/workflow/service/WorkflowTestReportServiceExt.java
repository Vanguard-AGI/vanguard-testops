package io.vanguard.testops.workflow.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import io.vanguard.testops.workflow.domain.WorkflowTestReport;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import io.vanguard.testops.workflow.mapper.WorkflowTestReportMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.support.page.PageUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流测试报告服务扩展
 * 包含测试报告列表、详情、工作流执行记录查询、删除、标签管理等功能
 */
@Slf4j
@Service
public class WorkflowTestReportServiceExt {

    @Resource
    private WorkflowTestReportMapper workflowTestReportMapper;

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    /**
     * 构建列表/统计共用查询条件。statusOverwrite 不为空时优先使用，否则用 request.getStatus()。
     */
    private LambdaQueryWrapper<WorkflowTestReport> buildListQueryWrapper(TestReportQueryRequest request, String statusOverwrite) {
        LambdaQueryWrapper<WorkflowTestReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(WorkflowTestReport::getDeletedTime);
        if (StringUtils.isNotBlank(request.getProjectId())) {
            queryWrapper.eq(WorkflowTestReport::getProjectId, request.getProjectId());
        }
        String status = statusOverwrite != null ? statusOverwrite : request.getStatus();
        if (StringUtils.isNotBlank(status)) {
            queryWrapper.eq(WorkflowTestReport::getStatus, status);
        }
        if (StringUtils.isNotBlank(request.getReportType())) {
            queryWrapper.eq(WorkflowTestReport::getReportType, request.getReportType());
        }
        if (request.getStartTime() != null) {
            queryWrapper.ge(WorkflowTestReport::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le(WorkflowTestReport::getCreateTime, request.getEndTime());
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            String keyword = "%" + request.getKeyword() + "%";
            queryWrapper.and(w -> w
                .like(WorkflowTestReport::getReportName, keyword)
                .or().like(WorkflowTestReport::getReportId, keyword)
                .or().like(WorkflowTestReport::getExecutor, keyword)
            );
        }
        queryWrapper.orderByDesc(WorkflowTestReport::getCreateTime);
        return queryWrapper;
    }

    /**
     * 获取当前筛选条件下的顶部统计（总报告数、已完成/运行中/失败为工作流维度、平均成功率=成功工作流/总工作流）
     */
    public TestReportStatsDTO getTestReportStats(TestReportQueryRequest request) {
        String projectId = request.getProjectId();
        if (StringUtils.isBlank(projectId)) {
            TestReportStatsDTO empty = new TestReportStatsDTO();
            empty.setTotal(0L);
            empty.setCompleted(0L);
            empty.setRunning(0L);
            empty.setFailed(0L);
            empty.setAvgSuccessRate(0.0);
            return empty;
        }
        long totalReports = workflowTestReportMapper.selectCount(buildListQueryWrapper(request, null));
        List<String> reportIds = workflowTestReportMapper.selectList(
                buildListQueryWrapper(request, null).select(WorkflowTestReport::getReportId))
                .stream().map(WorkflowTestReport::getReportId).collect(Collectors.toList());

        long completedWorkflows = 0;
        long runningWorkflows = 0;
        long failedWorkflows = 0;
        long totalWorkflows = 0;
        if (!reportIds.isEmpty()) {
            totalWorkflows = nullToZero(workflowRunMapper.selectCount(
                    new LambdaQueryWrapper<WorkflowRun>().in(WorkflowRun::getReportId, reportIds).isNull(WorkflowRun::getDeletedTime)));
            completedWorkflows = nullToZero(workflowRunMapper.selectCount(
                    new LambdaQueryWrapper<WorkflowRun>().in(WorkflowRun::getReportId, reportIds).isNull(WorkflowRun::getDeletedTime).eq(WorkflowRun::getStatus, "SUCCESS")));
            runningWorkflows = nullToZero(workflowRunMapper.selectCount(
                    new LambdaQueryWrapper<WorkflowRun>().in(WorkflowRun::getReportId, reportIds).isNull(WorkflowRun::getDeletedTime).eq(WorkflowRun::getStatus, "RUNNING")));
            failedWorkflows = nullToZero(workflowRunMapper.selectCount(
                    new LambdaQueryWrapper<WorkflowRun>().in(WorkflowRun::getReportId, reportIds).isNull(WorkflowRun::getDeletedTime).eq(WorkflowRun::getStatus, "FAILED")));
        }
        double avgSuccessRate = totalWorkflows > 0
                ? BigDecimal.valueOf(completedWorkflows * 100.0 / totalWorkflows).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 已完成=总工作流数，成功=成功工作流数(running 字段传)，失败=失败工作流数，平均成功率=成功/总数
        TestReportStatsDTO dto = new TestReportStatsDTO();
        dto.setTotal(totalReports);
        dto.setCompleted(totalWorkflows);   // 总的工作流数量
        dto.setRunning(completedWorkflows); // 成功的工作流总数（前端展示为「成功」）
        dto.setFailed(failedWorkflows);
        dto.setAvgSuccessRate(avgSuccessRate);
        return dto;
    }

    private static long nullToZero(Long v) {
        return v != null ? v : 0L;
    }

    public Pager<List<TestReportVO>> getTestReportPage(TestReportQueryRequest request) {
        LambdaQueryWrapper<WorkflowTestReport> queryWrapper = buildListQueryWrapper(request, null);
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        Page<WorkflowTestReport> page = (Page<WorkflowTestReport>) workflowTestReportMapper.selectList(queryWrapper);
        List<TestReportVO> voList = page.getResult().stream()
            .map(this::convertToTestReportVO)
            .collect(Collectors.toList());
        return PageUtils.setPageInfo(page, voList);
    }

    /**
     * 查询测试报告详情
     * 
     * @param reportId 报告ID
     * @return 报告详情
     */
    public TestReportDetailVO getTestReportDetail(String reportId) {
        WorkflowTestReport report = workflowTestReportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowTestReport>()
                .eq(WorkflowTestReport::getReportId, reportId)
                .isNull(WorkflowTestReport::getDeletedTime)
        );
        if (report == null) {
            throw new MSException("报告不存在");
        }
        return convertToTestReportDetailVO(report);
    }

    /**
     * 分页查询工作流执行记录列表
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    public Pager<List<WorkflowExecutionVO>> getWorkflowExecutionPage(WorkflowExecutionQueryRequest request) {
        if (StringUtils.isBlank(request.getReportId())) {
            throw new MSException("报告ID不能为空");
        }
        
        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowRun> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        // 软删除条件
        queryWrapper.isNull(WorkflowRun::getDeletedTime);
        
        // 报告ID筛选（必需）
        queryWrapper.eq(WorkflowRun::getReportId, request.getReportId());
        
        // 状态筛选
        if (StringUtils.isNotBlank(request.getStatus())) {
            // 将前端的 success/failed/running/cancelled 映射为数据库的 SUCCESS/FAILED/RUNNING/CANCELLED
            String dbStatus = mapFrontStatusToDbStatus(request.getStatus());
            queryWrapper.eq(WorkflowRun::getStatus, dbStatus);
        }
        
        // 关键词搜索（工作流名称）
        if (StringUtils.isNotBlank(request.getKeyword())) {
            queryWrapper.like(WorkflowRun::getWorkflowName, "%" + request.getKeyword() + "%");
        }
        
        // 排序：按开始时间倒序
        queryWrapper.orderByDesc(WorkflowRun::getStartTime);
        
        // 设置分页
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        Page<WorkflowRun> page = (Page<WorkflowRun>) workflowRunMapper.selectList(queryWrapper);
        
        // 转换为 VO
        List<WorkflowExecutionVO> voList = page.getResult().stream()
            .map(this::convertToWorkflowExecutionVO)
            .collect(Collectors.toList());
        
        return PageUtils.setPageInfo(page, voList);
    }

    /**
     * 删除测试报告（软删除）
     * 
     * @param reportId 报告ID
     */
    public void deleteTestReport(String reportId) {
        WorkflowTestReport report = workflowTestReportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowTestReport>()
                .eq(WorkflowTestReport::getReportId, reportId)
                .isNull(WorkflowTestReport::getDeletedTime)
        );
        if (report == null) {
            throw new MSException("报告不存在");
        }
        
        String userId = SessionUtils.getUserId();
        long currentTime = System.currentTimeMillis();
        
        // 软删除：设置删除时间和删除人
        report.setDeletedTime(currentTime);
        report.setDeletedBy(userId);
        workflowTestReportMapper.updateById(report);
        
        LogUtils.info("删除测试报告: reportId={}, operator={}", reportId, userId);
    }

    /**
     * 更新测试报告标签
     * 
     * @param reportId 报告ID
     * @param request 更新标签请求
     */
    public void updateTestReportTags(String reportId, UpdateTagsRequest request) {
        WorkflowTestReport report = workflowTestReportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowTestReport>()
                .eq(WorkflowTestReport::getReportId, reportId)
                .isNull(WorkflowTestReport::getDeletedTime)
        );
        if (report == null) {
            throw new MSException("报告不存在");
        }
        
        String userId = SessionUtils.getUserId();
        
        // 更新标签
        report.setTags(request.getTags());
        report.setUpdateTime(System.currentTimeMillis());
        report.setUpdateUser(userId);
        
        workflowTestReportMapper.updateById(report);
        
        LogUtils.info("更新测试报告标签: reportId={}, tags={}, operator={}", reportId, request.getTags(), userId);
    }

    /**
     * 更新测试报告名称
     * 
     * @param reportId 报告ID
     * @param request 更新名称请求
     */
    public void updateTestReportName(String reportId, UpdateReportNameRequest request) {
        WorkflowTestReport report = workflowTestReportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowTestReport>()
                .eq(WorkflowTestReport::getReportId, reportId)
                .isNull(WorkflowTestReport::getDeletedTime)
        );
        if (report == null) {
            throw new MSException("报告不存在");
        }
        
        String userId = SessionUtils.getUserId();
        
        // 更新报告名称
        report.setReportName(request.getReportName());
        report.setUpdateTime(System.currentTimeMillis());
        report.setUpdateUser(userId);
        
        workflowTestReportMapper.updateById(report);
        
        LogUtils.info("更新测试报告名称: reportId={}, reportName={}, operator={}", reportId, request.getReportName(), userId);
    }

    /**
     * 将 WorkflowTestReport 转换为 TestReportVO
     */
    private TestReportVO convertToTestReportVO(WorkflowTestReport report) {
        TestReportVO vo = new TestReportVO();
        vo.setReportId(report.getReportId());
        vo.setProjectId(report.getProjectId());
        vo.setReportName(report.getReportName());
        vo.setReportType(report.getReportType());
        vo.setTags(report.getTags());
        vo.setExecutor(report.getExecutor());
        vo.setTriggerType(report.getTriggerType());
        vo.setStatus(report.getStatus());
        vo.setStartTime(report.getStartTime());
        vo.setEndTime(report.getEndTime());
        vo.setDurationMs(report.getDurationMs()); // 报告生成耗时
        vo.setExecutionDurationMs(report.getExecutionDurationMs()); // 执行时长
        vo.setTotalWorkflows(report.getTotalWorkflows());
        vo.setTotalTests(report.getTotalTests());
        vo.setSuccessTests(report.getSuccessTests());
        vo.setSuccessWorkflows(getSuccessWorkflowsFromReport(report));
        vo.setFailedTests(report.getFailedTests());
        vo.setFailedWorkflows(getFailedWorkflowsFromReport(report));
        vo.setSkippedTests(report.getSkippedTests());
        vo.setPendingTests(report.getPendingTests());
        vo.setSuccessRate(report.getSuccessRate());
        vo.setAvgDurationSeconds(report.getAvgDurationSeconds());
        vo.setSummary(report.getSummary());
        vo.setEnvironmentId(report.getEnvironmentId());
        vo.setEnvironmentName(report.getEnvironmentName());
        vo.setCreateTime(report.getCreateTime());
        vo.setUpdateTime(report.getUpdateTime());
        return vo;
    }

    /**
     * 成功的工作流个数：优先从 resultSummary.completedCount 取，缺失时按 reportId 查 run 表统计。
     */
    private Integer getSuccessWorkflowsFromReport(WorkflowTestReport report) {
        if (report.getResultSummary() != null) {
            Object completed = report.getResultSummary().get("completedCount");
            if (completed instanceof Number) {
                return ((Number) completed).intValue();
            }
        }
        Long count = workflowRunMapper.selectCount(
                new LambdaQueryWrapper<WorkflowRun>()
                        .eq(WorkflowRun::getReportId, report.getReportId())
                        .eq(WorkflowRun::getStatus, "SUCCESS")
                        .isNull(WorkflowRun::getDeletedTime));
        return count != null ? count.intValue() : 0;
    }

    /**
     * 失败的工作流个数：优先从 resultSummary.failedCount 取，缺失时按 reportId 查 run 表 status=FAILED 统计。
     */
    private Integer getFailedWorkflowsFromReport(WorkflowTestReport report) {
        if (report.getResultSummary() != null) {
            Object failed = report.getResultSummary().get("failedCount");
            if (failed instanceof Number) {
                return ((Number) failed).intValue();
            }
        }
        Long count = workflowRunMapper.selectCount(
                new LambdaQueryWrapper<WorkflowRun>()
                        .eq(WorkflowRun::getReportId, report.getReportId())
                        .eq(WorkflowRun::getStatus, "FAILED")
                        .isNull(WorkflowRun::getDeletedTime));
        return count != null ? count.intValue() : 0;
    }

    /**
     * 将 WorkflowTestReport 转换为 TestReportDetailVO
     */
    private TestReportDetailVO convertToTestReportDetailVO(WorkflowTestReport report) {
        TestReportDetailVO vo = new TestReportDetailVO();
        vo.setReportId(report.getReportId());
        vo.setProjectId(report.getProjectId());
        vo.setReportName(report.getReportName());
        vo.setReportType(report.getReportType());
        vo.setTags(report.getTags());
        vo.setExecutor(report.getExecutor());
        vo.setTriggerType(report.getTriggerType());
        vo.setStatus(report.getStatus());
        vo.setStartTime(report.getStartTime());
        vo.setEndTime(report.getEndTime());
        vo.setDurationMs(report.getDurationMs());
        vo.setTotalWorkflows(report.getTotalWorkflows());
        vo.setTotalTests(report.getTotalTests());
        vo.setSuccessTests(report.getSuccessTests());
        vo.setSuccessWorkflows(getSuccessWorkflowsFromReport(report));
        vo.setFailedTests(report.getFailedTests());
        vo.setFailedWorkflows(getFailedWorkflowsFromReport(report));
        vo.setSkippedTests(report.getSkippedTests());
        vo.setPendingTests(report.getPendingTests());
        vo.setSuccessRate(report.getSuccessRate());
        vo.setAvgDurationSeconds(report.getAvgDurationSeconds());
        vo.setSummary(report.getSummary());
        vo.setResultSummary(report.getResultSummary());
        vo.setEnvironmentId(report.getEnvironmentId());
        vo.setEnvironmentName(report.getEnvironmentName());
        vo.setReportFileId(report.getReportFileId());
        vo.setCreateTime(report.getCreateTime());
        vo.setCreateUser(report.getCreateUser());
        vo.setUpdateTime(report.getUpdateTime());
        vo.setUpdateUser(report.getUpdateUser());
        return vo;
    }

    /**
     * 将 WorkflowRun 转换为 WorkflowExecutionVO
     */
    private WorkflowExecutionVO convertToWorkflowExecutionVO(WorkflowRun run) {
        WorkflowExecutionVO vo = new WorkflowExecutionVO();
        vo.setId(run.getRunId()); // 前端需要的 id 字段
        vo.setRunId(run.getRunId());
        vo.setReportId(run.getReportId());
        vo.setWorkflowId(run.getWorkflowId());
        vo.setWorkflowName(run.getWorkflowName());
        vo.setStatus(mapDbStatusToFrontStatus(run.getStatus()));
        vo.setStartTime(run.getStartTime());
        vo.setEndTime(run.getEndTime());
        
        // 耗时转换为秒（保留小数）
        if (run.getDurationMs() != null) {
            vo.setDuration(run.getDurationMs() / 1000.0);
        }
        
        vo.setTotalNodes(run.getTotalSteps());
        vo.setSuccessNodes(run.getPassedCount());
        vo.setFailedNodes(run.getFailedCount());
        vo.setSkippedNodes(run.getSkippedCount());
        vo.setPendingNodes(run.getPendingCount());
        vo.setExecutor(run.getTriggerUser());
        vo.setEnvironmentId(run.getEnvironmentId());
        vo.setEnvironmentName(run.getEnvironmentName());
        return vo;
    }

    /**
     * 将数据库状态映射为前端状态
     * SUCCESS/FAILED/RUNNING/CANCELLED -> success/failed/running/cancelled
     */
    private String mapDbStatusToFrontStatus(String dbStatus) {
        if ("SUCCESS".equals(dbStatus)) {
            return "success";
        } else if ("FAILED".equals(dbStatus)) {
            return "failed";
        } else if ("RUNNING".equals(dbStatus)) {
            return "running";
        } else if ("CANCELLED".equals(dbStatus)) {
            return "cancelled";
        }
        return "running"; // 默认值
    }

    /**
     * 将前端状态映射为数据库状态
     * success/failed/running/cancelled -> SUCCESS/FAILED/RUNNING/CANCELLED
     */
    private String mapFrontStatusToDbStatus(String frontStatus) {
        if ("success".equals(frontStatus)) {
            return "SUCCESS";
        } else if ("failed".equals(frontStatus)) {
            return "FAILED";
        } else if ("running".equals(frontStatus)) {
            return "RUNNING";
        } else if ("cancelled".equals(frontStatus)) {
            return "CANCELLED";
        }
        return "RUNNING"; // 默认值
    }
}
