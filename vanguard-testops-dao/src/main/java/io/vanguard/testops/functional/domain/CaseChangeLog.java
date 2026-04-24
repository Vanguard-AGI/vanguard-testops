package io.vanguard.testops.functional.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Data
public class CaseChangeLog implements Serializable {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用例ID")
    private String caseId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "变更原因: REQUIREMENT_TEMP(需求临时变更)/REQUIREMENT_ITERATION(需求迭代变更)/CASE_DESIGN(用例设计变更)/CASE_MAINTENANCE(历史用例维护)/TECH_SOLUTION(技术方案适配)/RESOURCE_ADJUSTMENT(资源配置调整)/EXTERNAL_DEPENDENCY(外部依赖变更)/COMPLIANCE_POLICY(合规政策要求)/SYS_DESIGN_CHANGE(系分变更)/CASE_COPY(用例复用)，其中 CASE_COPY 不参与用例变更原因分布统计")
    private String changeReason;

    @Schema(description = "变更区域: TITLE/STEP/EXPECTED")
    private String changeArea;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "变更时间")
    private Long createTime;

    private static final long serialVersionUID = 1L;
}
