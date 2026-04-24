package io.vanguard.testops.functional.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PersonalStatsDTO {
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "贡献用例数")
    private Integer caseCount;

    @Schema(description = "平均复杂度")
    private BigDecimal avgComplexity;

    @Schema(description = "平均UQS")
    private BigDecimal avgUqs;
    
    @Schema(description = "总编写工时(小时)")
    private BigDecimal totalWriteHours;
}
