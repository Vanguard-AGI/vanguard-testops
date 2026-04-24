package io.vanguard.testops.api.listener;

import io.vanguard.testops.api.invoker.ApiExecuteCallbackServiceInvoker;
import io.vanguard.testops.api.mapper.ApiReportMapper;
import io.vanguard.testops.api.mapper.ApiScenarioReportMapper;
import io.vanguard.testops.api.service.ApiBatchRunBaseService;
import io.vanguard.testops.api.service.ApiReportSendNoticeService;
import io.vanguard.testops.api.service.definition.ApiTestCaseBatchRunService;
import io.vanguard.testops.api.service.queue.ApiExecutionQueueService;
import io.vanguard.testops.api.service.scenario.ApiScenarioBatchRunService;
import io.vanguard.testops.sdk.constants.ApiExecuteResourceType;
import io.vanguard.testops.sdk.constants.ExecStatus;
import io.vanguard.testops.sdk.constants.KafkaTopicConstants;
import io.vanguard.testops.sdk.constants.ResultStatus;
import io.vanguard.testops.sdk.dto.api.notice.ApiNoticeDTO;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueue;
import io.vanguard.testops.sdk.dto.queue.ExecutionQueueDetail;
import io.vanguard.testops.sdk.util.EnumValidator;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    public static final String MESSAGE_CONSUME_ID = "MS-API-MESSAGE-CONSUME";
    @Resource
    private ApiReportSendNoticeService apiReportSendNoticeService;
    @Resource
    private ApiExecutionQueueService apiExecutionQueueService;
    @Resource
    private ApiBatchRunBaseService apiBatchRunBaseService;
    @Resource
    private ApiTestCaseBatchRunService apiTestCaseBatchRunService;
    @Resource
    private ApiScenarioBatchRunService apiScenarioBatchRunService;
    @Resource
    private ApiReportMapper apiReportMapper;
    @Resource
    private ApiScenarioReportMapper apiScenarioReportMapper;

    @KafkaListener(id = MESSAGE_CONSUME_ID, topics = KafkaTopicConstants.API_REPORT_TASK_TOPIC, groupId = MESSAGE_CONSUME_ID)
    public void messageConsume(ConsumerRecord<?, String> record) {
        try {
            if (ObjectUtils.isNotEmpty(record.value())) {
                ApiNoticeDTO dto = JSON.parseObject(record.value(), ApiNoticeDTO.class);
                LogUtils.info("接收到发送通知信息：{}", dto.getReportId());
                if (BooleanUtils.isTrue(dto.getRunModeConfig().isIntegratedReport())) {
                    ApiExecuteResourceType resourceType = EnumValidator.validateEnum(ApiExecuteResourceType.class, dto.getResourceType());
                    boolean isStop = false;
                    switch (resourceType) {
                        case API_CASE:
                        case TEST_PLAN_API_CASE:
                        case PLAN_RUN_API_CASE:
                            isStop = StringUtils.equals(apiReportMapper.selectByPrimaryKey(dto.getReportId()).getExecStatus(), ExecStatus.STOPPED.name())
                                    && deleteQueue(dto.getQueueId());
                            break;
                        case API_SCENARIO:
                        case TEST_PLAN_API_SCENARIO:
                        case PLAN_RUN_API_SCENARIO:
                            isStop = StringUtils.equals(apiScenarioReportMapper.selectByPrimaryKey(dto.getReportId()).getExecStatus(), ExecStatus.STOPPED.name())
                                    && deleteQueue(dto.getQueueId());
                            break;
                        default:
                            break;
                    }

                    if (isStop) {
                        return;
                    }
                    // 集合报告不发送通知
                } else {
                    try {
                        apiReportSendNoticeService.sendNotice(dto);
                    } catch (Exception e) {
                        LogUtils.error(e);
                    }
                }

                if (dto.getRunModeConfig().isSerial()) {
                    // 执行串行的下一个任务
                    executeNextTask(dto);
                }

                // 执行下个测试集
                executeNextCollection(dto);
            }
        } catch (Exception e) {
            LogUtils.error("接收到发送通知信息：{}", e);
        }
    }

    /**
     * 执行下一个测试集
     * @param
     *
     */
    private void executeNextCollection(ApiNoticeDTO dto) {
        if (BooleanUtils.isTrue(dto.getChildCollectionExecuteOver()) || isStopOnFailure(dto)) {
            // 如果当前测试集执行完了，或者当前测试集失败停止了，执行下一个测试集
            ApiExecuteCallbackServiceInvoker.executeNextCollection(dto, isStopOnFailure(dto));
        }
    }

    private boolean deleteQueue(String queueId) {
        apiExecutionQueueService.deleteQueue(queueId);
        return true;
    }

    /**
     * 执行批量的下一个任务
     *
     * @param dto
     */
    private void executeNextTask(ApiNoticeDTO dto) {
        try {
            ExecutionQueue queue = apiExecutionQueueService.getQueue(dto.getQueueId());
            if (queue == null) {
                return;
            }
            if (isStopOnFailure(dto)) {
                ApiExecuteResourceType resourceType = EnumValidator.validateEnum(ApiExecuteResourceType.class, queue.getResourceType());
                if (resourceType != ApiExecuteResourceType.PLAN_RUN_API_SCENARIO && resourceType != ApiExecuteResourceType.PLAN_RUN_API_CASE) {
                    // 失败停止，更新任务状态
                    apiBatchRunBaseService.updateTaskCompletedStatus(queue.getTaskId(), ResultStatus.ERROR.name());
                }
                // 补充集成报告
                updateStopOnFailureIntegratedReport(dto, queue, resourceType);
                // 如果是失败停止，清空队列，不继续执行
                apiExecutionQueueService.deleteQueue(queue.getQueueId());
                // 失败停止，删除父队列等
                ApiExecuteCallbackServiceInvoker.stopCollectionOnFailure(dto.getResourceType(), dto.getParentQueueId());
            } else {
                // queue 不为 null 说明有下个任务
                ExecutionQueueDetail nextDetail = apiExecutionQueueService.getNextDetail(dto.getQueueId());
                if (nextDetail != null) {
                    // 执行下个任务
                    ApiExecuteCallbackServiceInvoker.executeNextTask(queue.getResourceType(), queue, nextDetail);
                }
            }
        } catch (Exception e) {
            // 串行执行异常，清空队列
            if (StringUtils.isNotBlank(dto.getQueueId())) {
                apiExecutionQueueService.deleteQueue(dto.getQueueId());
            }
            if (StringUtils.isNotBlank(dto.getParentQueueId())) {
                apiExecutionQueueService.deleteQueue(dto.getParentQueueId());
            }
            LogUtils.error("执行任务失败：", e);
        }
    }

    /**
     * 处理失败停止后的报告处理
     *
     * @param dto
     * @param queue
     * @param resourceType
     * @return
     */
    private void updateStopOnFailureIntegratedReport(ApiNoticeDTO dto, ExecutionQueue queue, ApiExecuteResourceType resourceType) {
        if (dto.getRunModeConfig().isIntegratedReport()) {
            // 集成报告更新报告状态
            switch (resourceType) {
                case API_CASE:
                    apiTestCaseBatchRunService.updateStopOnFailureApiReport(queue);
                    break;
                case API_SCENARIO:
                    apiScenarioBatchRunService.updateStopOnFailureReport(queue);
                    break;
                default:
                    break;
            }
        }
    }

    private boolean isStopOnFailure(ApiNoticeDTO dto) {
        return BooleanUtils.isTrue(dto.getRunModeConfig().getStopOnFailure()) && StringUtils.equals(dto.getReportStatus(), ResultStatus.ERROR.name());
    }
}
