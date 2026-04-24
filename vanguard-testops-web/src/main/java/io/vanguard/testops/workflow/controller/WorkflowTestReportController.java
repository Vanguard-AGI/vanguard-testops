package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.service.WorkflowTestReportServiceExt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流测试报告 Controller
 */
@RestController
@RequestMapping("/workflow-test-report")
@Tag(name = "工作流测试报告管理")
public class WorkflowTestReportController {

    @Resource
    private WorkflowTestReportServiceExt workflowTestReportServiceExt;

    /**
     * 分页查询测试报告列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询测试报告列表")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_READ)
    public Pager<List<TestReportVO>> getTestReportPage(@RequestBody @Validated TestReportQueryRequest request) {
        String projectId = SessionUtils.getCurrentProjectId();
        if (org.apache.commons.lang3.StringUtils.isBlank(projectId)) {
            throw new io.vanguard.testops.sdk.exception.MSException("未选择项目");
        }
        request.setProjectId(projectId);
        LogUtils.info("分页查询测试报告列表: {}", request);
        return workflowTestReportServiceExt.getTestReportPage(request);
    }

    /**
     * 获取测试报告顶部统计（总报告数、已完成、运行中、失败、平均成功率），与列表使用相同筛选条件
     */
    @PostMapping("/stats")
    @Operation(summary = "获取测试报告统计")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_READ)
    public TestReportStatsDTO getTestReportStats(@RequestBody @Validated TestReportQueryRequest request) {
        String projectId = SessionUtils.getCurrentProjectId();
        if (org.apache.commons.lang3.StringUtils.isBlank(projectId)) {
            throw new io.vanguard.testops.sdk.exception.MSException("未选择项目");
        }
        request.setProjectId(projectId);
        return workflowTestReportServiceExt.getTestReportStats(request);
    }

    /**
     * 查询测试报告详情
     */
    @GetMapping("/detail/{reportId}")
    @Operation(summary = "查询测试报告详情")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_READ)
    public TestReportDetailVO getTestReportDetail(@PathVariable String reportId) {
        LogUtils.info("查询测试报告详情: reportId={}", reportId);
        return workflowTestReportServiceExt.getTestReportDetail(reportId);
    }

    /**
     * 分页查询工作流执行记录列表
     */
    @PostMapping("/executions/page")
    @Operation(summary = "分页查询工作流执行记录列表")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_READ)
    public Pager<List<WorkflowExecutionVO>> getWorkflowExecutionPage(@RequestBody @Validated WorkflowExecutionQueryRequest request) {
        LogUtils.info("分页查询工作流执行记录列表: {}", request);
        return workflowTestReportServiceExt.getWorkflowExecutionPage(request);
    }

    /**
     * 删除测试报告
     */
    @PostMapping("/delete/{reportId}")
    @Operation(summary = "删除测试报告")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_DELETE)
    public void deleteTestReport(@PathVariable String reportId) {
        String userId = SessionUtils.getUserId();
        LogUtils.info("删除测试报告: reportId={}, operator={}", reportId, userId);
        workflowTestReportServiceExt.deleteTestReport(reportId);
    }

    /**
     * 更新测试报告标签
     */
    @PostMapping("/tags/{reportId}")
    @Operation(summary = "更新测试报告标签")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_UPDATE)
    public void updateTestReportTags(@PathVariable String reportId, 
                                     @RequestBody @Validated UpdateTagsRequest request) {
        String userId = SessionUtils.getUserId();
        LogUtils.info("更新测试报告标签: reportId={}, tags={}, operator={}", reportId, request.getTags(), userId);
        workflowTestReportServiceExt.updateTestReportTags(reportId, request);
    }

    /**
     * 更新测试报告名称
     */
    @PostMapping("/name/{reportId}")
    @Operation(summary = "更新测试报告名称")
    @RequiresPermissions(PermissionConstants.PROJECT_WORKFLOW_TEST_REPORT_UPDATE)
    public void updateTestReportName(@PathVariable String reportId, 
                                     @RequestBody @Validated UpdateReportNameRequest request) {
        String userId = SessionUtils.getUserId();
        LogUtils.info("更新测试报告名称: reportId={}, reportName={}, operator={}", reportId, request.getReportName(), userId);
        workflowTestReportServiceExt.updateTestReportName(reportId, request);
    }
}
