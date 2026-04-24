package io.vanguard.testops.requirementquality.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

/**
 * 需求质量视图 - 列表查询请求
 */
@Data
public class RequirementQualityListRequest {

    @Schema(description = "项目ID，不传或空=全部项目（兼容旧版单选）")
    private String projectId;

    @Schema(description = "项目ID列表，不传或空=全部项目；多选时传此字段")
    private List<String> projectIds;

    @Schema(description = "需求列表ID；也可用 storyIds 传需求ID列表，不传或空=全部需求")
    private String requirementListId;

    @Schema(description = "需求ID列表，不传或空=全部需求")
    private List<String> storyIds;

    @Schema(description = "状态，不传或空=全部")
    private String status;

    @Min(value = 1, message = "当前页码必须大于0")
    @Schema(description = "页码，从1开始", requiredMode = Schema.RequiredMode.REQUIRED)
    private int current = 1;

    @Min(value = 1, message = "每页条数必须不小于1")
    @Max(value = 500, message = "每页条数不能大于500")
    @Schema(description = "每页条数", requiredMode = Schema.RequiredMode.REQUIRED)
    private int pageSize = 10;

    @Schema(description = "执行周期筛选：开始时间戳(毫秒)，需求执行周期与此区间有重叠时保留")
    private Long executionPeriodStart;

    @Schema(description = "执行周期筛选：结束时间戳(毫秒)，需求执行周期与此区间有重叠时保留")
    private Long executionPeriodEnd;

    @Schema(description = "排序字段：caseExecutedCount|executionRate|passRate|defectCount|reopenRate|codeCoverage，不传默认按更新时间")
    private String sortBy;

    @Schema(description = "排序方向：asc|desc，不传默认 desc")
    private String sortOrder;
}
