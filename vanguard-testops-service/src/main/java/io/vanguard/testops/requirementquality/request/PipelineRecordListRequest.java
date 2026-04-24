package io.vanguard.testops.requirementquality.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 门禁管理 - 流水线记录列表请求（分页 + 筛选）
 */
@Data
public class PipelineRecordListRequest {

    @NotNull(message = "当前页不能为空")
    @Schema(description = "当前页", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer current = 1;

    @NotNull(message = "每页条数不能为空")
    @Schema(description = "每页条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer pageSize = 20;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "需求ID（story_id），筛选该需求下关联的流水线")
    private String storyId;

    @Schema(description = "代码仓库名称（模糊）")
    private String repoName;

    @Schema(description = "发布结果：PENDING=待补全，SUCCESS/FAILED/ROLLED_BACK/HOTFIX 等，不传查全部")
    private String deployResult;

    @Schema(description = "发布时间起始（毫秒时间戳）")
    private Long deployTimeStart;

    @Schema(description = "发布时间结束（毫秒时间戳）")
    private Long deployTimeEnd;
}
