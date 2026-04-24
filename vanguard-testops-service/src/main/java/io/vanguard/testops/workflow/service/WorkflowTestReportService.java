package io.vanguard.testops.workflow.service;

import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import io.vanguard.testops.workflow.domain.WorkflowTestReport;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import io.vanguard.testops.workflow.mapper.WorkflowTestReportMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流测试报告服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowTestReportService {

    @Resource
    private WorkflowTestReportMapper workflowTestReportMapper;

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    /**
     * 创建测试报告（批量执行时调用）
     * 
     * @param projectId 项目ID
     * @param workflowIds 工作流ID列表
     * @param reportName 报告名称
     * @param executor 执行人
     * @param environmentId 环境ID
     * @param environmentName 环境名称
     * @param triggerType 触发类型: MANUAL(手动)/SCHEDULE(定时)/API(接口触发)
     * @return 报告ID
     */
    public String createReport(String projectId, List<String> workflowIds, String reportName, 
                                String executor, String environmentId, String environmentName, 
                                String triggerType) {
        if (CollectionUtils.isEmpty(workflowIds)) {
            throw new MSException("工作流ID列表不能为空");
        }

        long currentTime = System.currentTimeMillis();
        WorkflowTestReport report = new WorkflowTestReport();
        report.setReportId(IDGenerator.nextStr());
        report.setProjectId(projectId);
        if (StringUtils.isNotBlank(reportName)) {
            report.setReportName(reportName);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String defaultReportName = "RPT-" + sdf.format(new Date(currentTime));
            report.setReportName(defaultReportName);
        }
        // 根据 triggerType 设置 reportType
        String normalizedTriggerType = StringUtils.defaultIfBlank(triggerType, "MANUAL");
        report.setReportType(mapTriggerTypeToReportType(normalizedTriggerType));
        report.setExecutor(executor);
        report.setTriggerType(normalizedTriggerType);
        report.setStatus("RUNNING");
        report.setStartTime(currentTime);
        report.setTotalWorkflows(workflowIds.size());
        report.setTotalTests(0);
        report.setSuccessTests(0);
        report.setFailedTests(0);
        report.setSkippedTests(0);
        report.setPendingTests(0);
        report.setEnvironmentId(environmentId);
        report.setEnvironmentName(environmentName);
        report.setCreateTime(currentTime);
        report.setCreateUser(executor);
        report.setUpdateTime(currentTime);
        report.setUpdateUser(executor);

        // 初始化结果摘要，记录所有工作流ID
        Map<String, Object> resultSummary = new HashMap<>();
        resultSummary.put("workflowIds", workflowIds);
        resultSummary.put("workflowStatus", new HashMap<>());
        report.setResultSummary(resultSummary);

        workflowTestReportMapper.insert(report);
        return report.getReportId();
    }

    /**
     * 更新报告状态（当工作流执行完成时调用）
     * 
     * @param reportId 报告ID
     */
    public void updateReportOnWorkflowComplete(String reportId) {
        WorkflowTestReport report = workflowTestReportMapper.selectByReportId(reportId);
        if (report == null) {
            return;
        }

        // 查询该报告关联的所有工作流运行记录（只查询未删除的记录）
        List<WorkflowRun> runs = workflowRunMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowRun>()
                .eq(WorkflowRun::getReportId, reportId)
                .isNull(WorkflowRun::getDeletedTime)  // 只统计未删除的运行记录
        );

        if (CollectionUtils.isEmpty(runs)) {
            return;
        }

        // 聚合统计数据
        int totalTests = 0;
        int successTests = 0;
        int failedTests = 0;
        int skippedTests = 0;
        int pendingTests = 0;
        long executionDurationMs = 0; // 执行时长：所有 workflow 执行耗时的总和
        long totalDuration = 0; // 用于计算平均耗时
        int completedCount = 0;
        int failedCount = 0;
        int runningCount = 0;
        int pendingCount = 0;

        Map<String, Object> workflowStatus = new HashMap<>();
        List<Long> durations = new ArrayList<>();

        for (WorkflowRun run : runs) {
            // 统计测试数
            if (run.getTotalSteps() != null) {
                totalTests += run.getTotalSteps();
            }
            if (run.getPassedCount() != null) {
                successTests += run.getPassedCount();
            }
            if (run.getFailedCount() != null) {
                failedTests += run.getFailedCount();
            }
            if (run.getSkippedCount() != null) {
                skippedTests += run.getSkippedCount();
            }
            if (run.getPendingCount() != null) {
                pendingTests += run.getPendingCount();
            }

            // 累加执行时长（所有 workflow 的 durationMs 总和）
            // 注意：使用 workflow 级别的 durationMs，不是节点级别的
            if (run.getDurationMs() != null) {
                executionDurationMs += run.getDurationMs();
            }

            // 统计工作流状态
            String status = run.getStatus();
            Map<String, Object> runStatus = new HashMap<>();
            runStatus.put("runId", run.getRunId());
            runStatus.put("workflowId", run.getWorkflowId());
            runStatus.put("workflowName", run.getWorkflowName());
            runStatus.put("status", status);
            runStatus.put("startTime", run.getStartTime());
            runStatus.put("endTime", run.getEndTime());
            runStatus.put("durationMs", run.getDurationMs());
            workflowStatus.put(run.getWorkflowId(), runStatus);

            if ("SUCCESS".equals(status)) {
                completedCount++;
                if (run.getDurationMs() != null) {
                    durations.add(run.getDurationMs());
                    totalDuration += run.getDurationMs();
                }
            } else if ("FAILED".equals(status)) {
                failedCount++;
                if (run.getDurationMs() != null) {
                    durations.add(run.getDurationMs());
                    totalDuration += run.getDurationMs();
                }
            } else if ("RUNNING".equals(status)) {
                runningCount++;
            } else if ("PENDING".equals(status)) {
                pendingCount++;
            }
        }

        // 更新报告统计
        report.setTotalTests(totalTests);
        report.setSuccessTests(successTests);
        report.setFailedTests(failedTests);
        report.setSkippedTests(skippedTests);
        report.setPendingTests(pendingTests);
        
        // 设置执行时长（所有 workflow 执行耗时的总和）
        report.setExecutionDurationMs(executionDurationMs);

        // 计算成功率
        if (totalTests > 0) {
            BigDecimal successRate = BigDecimal.valueOf(successTests)
                .divide(BigDecimal.valueOf(totalTests), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            report.setSuccessRate(successRate);
        }

        // 计算平均执行时长
        if (!durations.isEmpty()) {
            long avgDuration = totalDuration / durations.size();
            report.setAvgDurationSeconds((int) (avgDuration / 1000));
        }

        // 更新结果摘要
        Map<String, Object> resultSummary = report.getResultSummary();
        if (resultSummary == null) {
            resultSummary = new HashMap<>();
        }
        resultSummary.put("workflowStatus", workflowStatus);
        resultSummary.put("completedCount", completedCount);
        resultSummary.put("failedCount", failedCount);
        resultSummary.put("runningCount", runningCount);
        resultSummary.put("pendingCount", pendingCount);
        report.setResultSummary(resultSummary);

        // 判断报告状态：只要所有工作流都完成（无论成功还是失败），报告状态就是 COMPLETED
        // 具体的成功/失败情况在报告详情中体现（通过 successTests、failedTests、completedCount、failedCount 等字段）
        if (runningCount > 0 || pendingCount > 0) {
            report.setStatus("RUNNING");
        } else {
            // 所有工作流都已完成，报告状态为 COMPLETED
            report.setStatus("COMPLETED");
            
            // 计算报告的结束时间和总耗时
            // 使用最后一个完成的 workflow 的 endTime 作为报告的 endTime
            Long latestEndTime = null;
            for (WorkflowRun run : runs) {
                if (run.getEndTime() != null) {
                    if (latestEndTime == null || run.getEndTime() > latestEndTime) {
                        latestEndTime = run.getEndTime();
                    }
                }
            }
            
            if (latestEndTime != null) {
                report.setEndTime(latestEndTime);
            } else {
                // 如果没有找到 endTime，使用当前时间
            report.setEndTime(System.currentTimeMillis());
            }
            
            // 计算报告生成耗时：从 reportId 生成时间（startTime）到所有 workflow 完成时间（endTime）的差值
            if (report.getStartTime() != null && report.getEndTime() != null) {
                report.setDurationMs(report.getEndTime() - report.getStartTime());
            }
        }

        report.setUpdateTime(System.currentTimeMillis());
        workflowTestReportMapper.updateById(report);
    }

    /**
     * 根据报告ID查询报告
     */
    public WorkflowTestReport getReport(String reportId) {
        return workflowTestReportMapper.selectByReportId(reportId);
    }

    /**
     * 将 triggerType 映射为 reportType
     * MANUAL -> MANUAL (手动生成)
     * SCHEDULE -> SCHEDULE (定时生成)
     * API -> AUTO (自动生成)
     */
    private String mapTriggerTypeToReportType(String triggerType) {
        if (StringUtils.isBlank(triggerType)) {
            return "MANUAL";
        }
        switch (triggerType.toUpperCase()) {
            case "MANUAL":
                return "MANUAL";
            case "SCHEDULE":
                return "SCHEDULE";
            case "API":
                return "AUTO";
            default:
                return "MANUAL"; // 默认值
        }
    }
}

