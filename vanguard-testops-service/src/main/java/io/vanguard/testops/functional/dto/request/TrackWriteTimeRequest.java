package io.vanguard.testops.functional.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TrackWriteTimeRequest {
    @Schema(description = "用例ID")
    private String caseId;

    @Schema(description = "持续时长(毫秒)")
    private Long durationMs;
}
