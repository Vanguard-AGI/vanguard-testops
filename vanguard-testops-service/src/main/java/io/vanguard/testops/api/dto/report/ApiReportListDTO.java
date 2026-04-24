package io.vanguard.testops.api.dto.report;

import io.vanguard.testops.api.domain.ApiReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApiReportListDTO extends ApiReport {
    @Schema(description = "创建人")
    private String createUserName;
    @Schema(description = "更新人")
    private String updateUserName;

}
