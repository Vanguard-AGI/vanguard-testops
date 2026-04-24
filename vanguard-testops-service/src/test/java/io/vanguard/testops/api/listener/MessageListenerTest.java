package io.vanguard.testops.api.listener;

import io.vanguard.testops.sdk.constants.ApiExecuteResourceType;
import io.vanguard.testops.sdk.constants.KafkaTopicConstants;
import io.vanguard.testops.sdk.dto.api.notice.ApiNoticeDTO;
import io.vanguard.testops.sdk.util.JSON;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
public class MessageListenerTest {

    @Resource
    private MessageListener messageListener;

    @Test
    void testDebugConsume() {
        // 模拟参数
        ApiNoticeDTO api = new ApiNoticeDTO();

        // Set values for the fields
        api.setResourceType(ApiExecuteResourceType.API.name());
        api.setResourceId("exampleResourceId");
        api.setReportStatus("exampleReportStatus");
        api.setUserId("exampleUserId");
        api.setProjectId("exampleProjectId");
        api.getRunModeConfig().setEnvironmentId("exampleEnvironmentId");
        api.setReportId("exampleReportId");

        ConsumerRecord<Object, String> record = new ConsumerRecord<>(KafkaTopicConstants.API_REPORT_TASK_TOPIC, 0, 0, "123", JSON.toJSONString(api));
        // 调用被测试方法
        messageListener.messageConsume(record);

        // 模拟参数
        ApiNoticeDTO scenario = new ApiNoticeDTO();

        // Set values for the fields
        scenario.setResourceType(ApiExecuteResourceType.API.name());
        scenario.setResourceId("exampleResourceId");
        scenario.setReportStatus("exampleReportStatus");
        scenario.setUserId("exampleUserId");
        scenario.setProjectId("exampleProjectId");
        api.getRunModeConfig().setEnvironmentId("exampleEnvironmentId");
        scenario.setReportId("exampleReportId");

        ConsumerRecord<Object, String> scenarioRecord = new ConsumerRecord<>(KafkaTopicConstants.API_REPORT_TASK_TOPIC, 0, 0, "123", JSON.toJSONString(scenario));
        // 调用被测试方法
        messageListener.messageConsume(scenarioRecord);

        // 模拟参数
        ApiNoticeDTO testCase = new ApiNoticeDTO();

        // Set values for the fields
        testCase.setResourceType(ApiExecuteResourceType.API.name());
        testCase.setResourceId("exampleResourceId");
        testCase.setReportStatus("exampleReportStatus");
        testCase.setUserId("exampleUserId");
        testCase.setProjectId("exampleProjectId");
        api.getRunModeConfig().setEnvironmentId("exampleEnvironmentId");
        testCase.setReportId("exampleReportId");

        ConsumerRecord<Object, String> testCaseRecord = new ConsumerRecord<>(KafkaTopicConstants.API_REPORT_TASK_TOPIC, 0, 0, "123", JSON.toJSONString(testCase));
        // 调用被测试方法
        messageListener.messageConsume(testCaseRecord);
    }
}
