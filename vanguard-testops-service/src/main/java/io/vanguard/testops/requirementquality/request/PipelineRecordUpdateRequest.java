package io.vanguard.testops.requirementquality.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 门禁管理 - 运维补全流水线记录（需求ID、项目、环境、发布结果等）
 */
@Data
public class PipelineRecordUpdateRequest {

    @NotBlank(message = "记录ID不能为空")
    @Schema(description = "流水线记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "需求ID（Story ID）")
    private String storyId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "环境")
    private String env;

    @Schema(description = "发布结果：SUCCESS/FAILED/ROLLED_BACK/HOTFIX")
    private String deployResult;

    @Schema(description = "是否回滚：0/1")
    private Integer isRollback;

    @Schema(description = "是否紧急补丁：0/1")
    private Integer isHotfix;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "前端负责人/开发")
    private String frontend;

    @Schema(description = "后端负责人/开发")
    private String backend;

    @Schema(description = "流水线详情链接，可用于纠正或补录")
    private String pipelineUrl;
}
