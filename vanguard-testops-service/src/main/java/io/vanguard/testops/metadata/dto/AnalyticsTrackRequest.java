package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "埋点追踪请求")
public class AnalyticsTrackRequest {

    @Schema(description = "事件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Event> events;

    @Data
    @Schema(description = "事件对象")
    public static class Event {
        @Schema(description = "事件名称（execute/update/create/delete）", requiredMode = Schema.RequiredMode.REQUIRED)
        private String event;

        @Schema(description = "事件属性", requiredMode = Schema.RequiredMode.REQUIRED)
        private Map<String, Object> properties;

        @Schema(description = "页面标识（DUBBO/HTTP等）")
        private String page;

        @Schema(description = "平台标识：AegisOne/Web→bizType=1；Plugin/AegisGo→bizType=2；Electron→bizType=3。2与3均按 AegisGO 逻辑处理")
        private String platform;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @Schema(description = "时间戳（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long timestamp;

        @Schema(description = "执行耗时（毫秒）")
        private Long duration;
    }
}

