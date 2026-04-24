package io.vanguard.testops.sdk.constants;

public class KafkaTopicConstants {
    public static final String PLUGIN = "PLUGIN";
    public static final String EXPORT = "EXPORT";
    // API TOPIC
    public static final String API_REPORT_TOPIC = "API_REPORT_TOPIC";
    public static final String API_REPORT_TASK_TOPIC = "API_REPORT_TASK_TOPIC";
    public static final String API_REPORT_DEBUG_TOPIC = "API_REPORT_DEBUG_TOPIC";

    /** 工作流执行结果（执行机发往平台，方案 B 避免回调死锁） */
    public static final String WORKFLOW_RUN_RESULT_TOPIC = "workflow-run-result";

    public static class TYPE {
        public static final String ADD = "ADD";
        public static final String DELETE = "DELETE";
    }
}
