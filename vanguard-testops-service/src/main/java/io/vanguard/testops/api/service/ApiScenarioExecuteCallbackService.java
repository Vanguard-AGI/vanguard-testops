package io.vanguard.testops.api.service;


import io.vanguard.testops.api.dto.scenario.ApiScenarioDetail;
import io.vanguard.testops.api.invoker.ApiExecuteCallbackServiceInvoker;
import io.vanguard.testops.api.service.scenario.ApiScenarioBatchRunService;
import io.vanguard.testops.api.service.scenario.ApiScenarioRunService;
import io.vanguard.testops.api.support.cache.ApiScenarioIntegratedReportStepCache;
import io.vanguard.testops.sdk.constants.ApiExecuteResourceType;
import io.vanguard.testops.sdk.constants.ApiExecuteRunMode;
import io.vanguard.testops.sdk.dto.api.task.GetRunScriptRequest;
import io.vanguard.testops.sdk.dto.api.task.GetRunScriptResult;
import io.vanguard.testops.sdk.dto.api.task.TaskItem;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueue;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueueDetail;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ApiScenarioExecuteCallbackService implements ApiExecuteCallbackService {
    @Resource
    private ApiScenarioRunService apiScenarioRunService;
    @Resource
    private ApiScenarioBatchRunService apiScenarioBatchRunService;
    @Resource
    private ApiScenarioIntegratedReportStepCache apiScenarioIntegratedReportStepCache;

    public ApiScenarioExecuteCallbackService() {
        ApiExecuteCallbackServiceInvoker.register(ApiExecuteResourceType.API_SCENARIO, this);
    }

    /**
     * 解析并返回执行脚本
     */
    @Override
    public GetRunScriptResult getRunScript(GetRunScriptRequest request) {
        ApiScenarioDetail apiScenarioDetail = apiScenarioRunService.getForRunWithTaskItemErrorMassage(request.getTaskItem().getId(), request.getTaskItem().getResourceId());
        String reportId = initReport(request, apiScenarioDetail);
        GetRunScriptResult result = apiScenarioRunService.getRunScript(request, apiScenarioDetail);
        result.setReportId(reportId);
        return result;
    }

    private String initReport(GetRunScriptRequest request, ApiScenarioDetail apiScenarioDetail) {
        TaskItem taskItem = request.getTaskItem();
        String reportId = taskItem.getReportId();
        try {
            if (BooleanUtils.isTrue(request.getBatch())) {
                if (request.getRunModeConfig().isIntegratedReport()) {
                    // 避免接口重试，步骤重复生成
                    if (apiScenarioIntegratedReportStepCache.setIfAbsent(reportId, apiScenarioDetail.getId())) {
                        // 集合报告，生成一级步骤的子步骤
                        apiScenarioRunService.initScenarioReportSteps(apiScenarioDetail.getId(), apiScenarioDetail.getSteps(), reportId);
                    }
                } else {
                    // 批量执行，生成独立报告
                    reportId = apiScenarioBatchRunService.initScenarioReport(taskItem.getId(), reportId, request.getRunModeConfig(), apiScenarioDetail, request.getUserId());
                    // 初始化报告步骤
                    apiScenarioRunService.initScenarioReportSteps(apiScenarioDetail.getSteps(), reportId);
                }
            } else if (!ApiExecuteRunMode.isDebug(request.getRunMode())) {
                reportId = apiScenarioRunService.initApiScenarioReport(taskItem.getId(), apiScenarioDetail, request);
                // 初始化报告步骤
                apiScenarioRunService.initScenarioReportSteps(apiScenarioDetail.getSteps(), reportId);
            }
        } catch (DuplicateKeyException e) {
            if (!request.getRunModeConfig().isIntegratedReport()) {
                // 步骤中的 stepId 是执行时时随机生成的，如果重试，需要删除原有的步骤，重新生成，跟执行脚本匹配
                apiScenarioRunService.deleteStepsByReportId(reportId);
                apiScenarioRunService.initScenarioReportSteps(apiScenarioDetail.getSteps(), reportId);
            }
            // 避免重试，报告ID重复，导致执行失败
            LogUtils.error(e);
        }
        return reportId;
    }

    /**
     * 串行时，执行下一个任务
     *
     * @param queue
     * @param queueDetail
     */
    @Override
    public void executeNextTask(ExecutionQueue queue, ExecutionQueueDetail queueDetail) {
        apiScenarioBatchRunService.executeNextTask(queue, queueDetail);
    }
}
