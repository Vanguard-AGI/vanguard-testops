package io.vanguard.testops.plan.service;

import io.vanguard.testops.api.controller.result.ApiResultCode;
import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.api.domain.ApiTestCaseRecord;
import io.vanguard.testops.api.invoker.ApiExecuteCallbackServiceInvoker;
import io.vanguard.testops.api.mapper.ApiTestCaseMapper;
import io.vanguard.testops.api.service.ApiCommonService;
import io.vanguard.testops.api.service.ApiExecuteCallbackService;
import io.vanguard.testops.api.service.definition.ApiTestCaseRunService;
import io.vanguard.testops.plan.domain.TestPlanApiCase;
import io.vanguard.testops.plan.mapper.TestPlanApiCaseMapper;
import io.vanguard.testops.sdk.constants.ApiExecuteResourceType;
import io.vanguard.testops.sdk.constants.TaskItemErrorMessage;
import io.vanguard.testops.sdk.dto.api.notice.ApiNoticeDTO;
import io.vanguard.testops.sdk.dto.api.task.GetRunScriptRequest;
import io.vanguard.testops.sdk.dto.api.task.GetRunScriptResult;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueue;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueueDetail;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanApiCaseExecuteCallbackService implements ApiExecuteCallbackService {
    @Resource
    private TestPlanApiCaseService testPlanApiCaseService;
    @Resource
    private TestPlanApiCaseBatchRunService testPlanApiCaseBatchRunService;
    @Resource
    private TestPlanApiCaseMapper testPlanApiCaseMapper;
    @Resource
    private ApiTestCaseMapper apiTestCaseMapper;
    @Resource
    private ApiTestCaseRunService apiTestCaseRunService;
    @Resource
    private ApiCommonService apiCommonService;

    public TestPlanApiCaseExecuteCallbackService() {
        ApiExecuteCallbackServiceInvoker.register(ApiExecuteResourceType.TEST_PLAN_API_CASE, this);
    }

    /**
     * 解析并返回执行脚本
     */
    @Override
    public GetRunScriptResult getRunScript(GetRunScriptRequest request) {
        TestPlanApiCase testPlanApiCase = testPlanApiCaseMapper.selectByPrimaryKey(request.getTaskItem().getResourceId());
        ApiTestCase apiTestCase = testPlanApiCase == null ? null : apiTestCaseMapper.selectByPrimaryKey(testPlanApiCase.getApiCaseId());
        if (testPlanApiCase == null || apiTestCase == null || apiTestCase.getDeleted()) {
            apiCommonService.updateTaskItemErrorMassage(request.getTaskItem().getId(), TaskItemErrorMessage.CASE_NOT_EXIST);
            throw new MSException(ApiResultCode.CASE_NOT_EXIST);
        }
        String reportId = initReport(request, testPlanApiCase, apiTestCase);
        GetRunScriptResult result = apiTestCaseRunService.getRunScript(request, apiTestCase);
        result.setReportId(reportId);
        return result;
    }

    public String initReport(GetRunScriptRequest request, TestPlanApiCase testPlanApiCase, ApiTestCase apiTestCase) {
        try {
            return testPlanApiCaseService.initApiReport(apiTestCase, testPlanApiCase, request);
        } catch (DuplicateKeyException e) {
            // 避免重试，报告ID重复，导致执行失败
            LogUtils.error(e);
        }
        return request.getTaskItem().getReportId();
    }

    /**
     * 串行时，执行下一个任务
     *
     * @param queue
     * @param queueDetail
     */
    @Override
    public void executeNextTask(ExecutionQueue queue, ExecutionQueueDetail queueDetail) {
        testPlanApiCaseBatchRunService.executeNextTask(queue, queueDetail);
    }

    /**
     * 批量串行的测试集执行时
     * 测试集下用例执行完成时回调
     */
    @Override
    public void executeNextCollection(ApiNoticeDTO apiNoticeDTO, boolean isStopOnFailure) {
        if (StringUtils.isNotBlank(apiNoticeDTO.getParentQueueId())) {
            testPlanApiCaseBatchRunService.executeNextCollection(apiNoticeDTO.getParentQueueId(), apiNoticeDTO.getRerun());
        } else if (StringUtils.isNotBlank(apiNoticeDTO.getParentSetId())) {
            String queueIdOrSetId = StringUtils.isBlank(apiNoticeDTO.getQueueId()) ? apiNoticeDTO.getSetId() : apiNoticeDTO.getQueueId();
            String[] setIdSplit = queueIdOrSetId.split("_");
            String collectionId = setIdSplit[setIdSplit.length - 1];
            testPlanApiCaseBatchRunService.finishParallelCollection(apiNoticeDTO.getParentSetId(), collectionId);
        }
    }

    /**
     * 失败停止时，删除测试集合队列
     *
     * @param parentQueueId
     */
    @Override
    public void stopCollectionOnFailure(String parentQueueId) {
        testPlanApiCaseBatchRunService.stopCollectionOnFailure(parentQueueId);
    }
}
