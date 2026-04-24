package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.WorkflowRunDTO;
import io.vanguard.testops.workflow.dto.WorkflowRunExecuteRequest;
import io.vanguard.testops.workflow.dto.WorkflowRunPageRequest;
import io.vanguard.testops.workflow.dto.WorkflowDebugNodeRequest;
import io.vanguard.testops.workflow.dto.WorkflowDebugPublicNodeRequest;
import io.vanguard.testops.workflow.support.callback.WorkflowRunResultCallbackRequest;
import io.vanguard.testops.workflow.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流运行 Controller
 */
@RestController
@RequestMapping("/workflow/run")
@Tag(name = "工作流运行管理")
public class WorkflowRunController {

    @Resource
    private WorkflowRunService workflowRunService;

    @PostMapping("/execute")
    @Operation(summary = "执行工作流")
    public WorkflowRunDTO execute(@RequestBody @Validated WorkflowRunExecuteRequest request) {
        return workflowRunService.execute(request, SessionUtils.getUserId());
    }

    @PostMapping("/debug/node")
    @Operation(summary = "调试单个节点")
    public Map<String, Object> debugNode(@RequestBody @Validated WorkflowDebugNodeRequest request) {
        return workflowRunService.debugNode(request, SessionUtils.getUserId());
    }

    @PostMapping("/debug/public-node")
    @Operation(summary = "调试公共节点（无工作流上下文）")
    public Map<String, Object> debugPublicNode(@RequestBody @Validated WorkflowDebugPublicNodeRequest request) {
        return workflowRunService.debugPublicNode(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/{runId}")
    @Operation(summary = "获取运行详情")
    public WorkflowRunDTO getRunDetail(@PathVariable String runId) {
        return workflowRunService.getRunDetail(runId);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询运行记录")
    public Pager<List<WorkflowRunDTO>> getRunList(@RequestBody @Validated WorkflowRunPageRequest request) {
        return workflowRunService.getRunList(request);
    }

    @PostMapping("/cancel/{runId}")
    @Operation(summary = "取消执行")
    public void cancel(@PathVariable String runId) {
        workflowRunService.cancel(runId, SessionUtils.getUserId());
    }

    @PostMapping("/delete/{runId}")
    @Operation(summary = "删除运行记录")
    public void delete(@PathVariable String runId) {
        workflowRunService.delete(runId, SessionUtils.getUserId());
    }

    @GetMapping("/log/run/{runId}/step/{runStepId}")
    @Operation(summary = "根据运行ID和运行步骤ID获取执行日志")
    public List<Map<String, Object>> getRunLogsByRunIdAndRunStepId(
            @PathVariable String runId,
            @PathVariable String runStepId) {
        return workflowRunService.getRunLogsByRunIdAndRunStepId(runId, runStepId);
    }

    @PostMapping("/result/callback")
    @Operation(summary = "执行结果回调接口（执行机调用）")
    public Map<String, Object> callback(@RequestBody @Validated WorkflowRunResultCallbackRequest request) {
        return workflowRunService.handleCallback(request);
    }
}

