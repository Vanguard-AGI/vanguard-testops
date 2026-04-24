package io.vanguard.testops.workflow.listener;

import io.vanguard.testops.sdk.constants.KafkaTopicConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.workflow.support.callback.WorkflowRunResultCallbackRequest;
import io.vanguard.testops.workflow.service.WorkflowRunService;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 消费执行机发送的工作流执行结果（方案 B：Kafka 结果 topic，避免 HTTP 回调死锁）。
 * 顺序消费后调用 {@link WorkflowRunService#applyWorkflowRunResult} 写库并推送 WebSocket。
 */
@Component
public class WorkflowRunResultKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunResultKafkaListener.class);

    public static final String CONSUMER_GROUP_ID = "workflow-run-result-consumer";

    @Resource
    private WorkflowRunService workflowRunService;

    @KafkaListener(
            id = CONSUMER_GROUP_ID,
            topics = KafkaTopicConstants.WORKFLOW_RUN_RESULT_TOPIC,
            groupId = CONSUMER_GROUP_ID
    )
    public void onWorkflowRunResult(ConsumerRecord<?, String> record) {
        String value = record.value();
        if (value == null || value.isBlank()) {
            log.warn("收到空的工作流结果消息: partition={}, offset={}", record.partition(), record.offset());
            return;
        }
        try {
            WorkflowRunResultCallbackRequest request = JSON.parseObject(value, WorkflowRunResultCallbackRequest.class);
            if (request == null) {
                log.warn("工作流结果反序列化为 null: partition={}, offset={}", record.partition(), record.offset());
                return;
            }
            workflowRunService.applyWorkflowRunResult(request);
        } catch (Exception e) {
            log.error("处理工作流结果消息失败: partition={}, offset={}, value={}, error={}",
                    record.partition(), record.offset(), value, e.getMessage(), e);
            throw e;
        }
    }
}
