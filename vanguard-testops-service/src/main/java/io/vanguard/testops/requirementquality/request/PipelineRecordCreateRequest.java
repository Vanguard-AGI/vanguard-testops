package io.vanguard.testops.requirementquality.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布管理 - 手动创建云效流水线记录（用户填写后落库）
 */
@Data
public class PipelineRecordCreateRequest {

    @NotBlank(message = "流水线ID不能为空")
    @Schema(description = "云效流水线运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pipelineId;

    @NotBlank(message = "流水线名称不能为空")
    @Schema(description = "流水线名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pipelineName;

    @NotBlank(message = "代码仓库名称不能为空")
    @Schema(description = "代码仓库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repoName;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "FRONTEND/BACKEND", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endpointType;

    @NotNull(message = "发布时间不能为空")
    @Schema(description = "发布时间（毫秒时间戳）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deployTime;

    @NotBlank(message = "发布人不能为空")
    @Schema(description = "发布人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deployer;

    @NotBlank(message = "发布结果不能为空")
    @Schema(description = "发布结果：SUCCESS/FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deployResult;

    @NotBlank(message = "需求不能为空")
    @Schema(description = "需求ID（Story ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storyId;

    @Schema(description = "需求名称（展示用，不落库）")
    private String storyName;

    @NotBlank(message = "项目不能为空")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @Schema(description = "项目名称（展示用，不落库）")
    private String projectName;

    @NotNull(message = "代码新增行数不能为空")
    @Schema(description = "代码新增行数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer locAdd;

    @NotNull(message = "代码删除行数不能为空")
    @Schema(description = "代码删除行数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer locDelete;

    @Schema(description = "是否回滚：0/1", example = "0")
    private Integer isRollback;

    @Schema(description = "是否热修：0/1", example = "0")
    private Integer isHotfix;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "前端负责人/开发")
    private String frontend;

    @Schema(description = "后端负责人/开发")
    private String backend;

    @NotBlank(message = "流水线链接")
    @Schema(description = "流水线详情链接", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pipelineUrl;
}
