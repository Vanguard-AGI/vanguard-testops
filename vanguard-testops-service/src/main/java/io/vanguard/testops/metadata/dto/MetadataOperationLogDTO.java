package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "操作日志DTO")
public class MetadataOperationLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "日志唯一标识")
    private String logId;

    @Schema(description = "日志类型：1=aegisOne 2=aegisGo")
    private Integer bizType;

    @Schema(description = "模块类型")
    private String moduleType;

    @Schema(description = "项目唯一标识")
    private String projectId;

    @Schema(description = "关联业务ID")
    private String relatedId;

    @Schema(description = "具体操作")
    private String action;

    @Schema(description = "操作人邮箱")
    private String userEmail;

    @Schema(description = "执行耗时（毫秒）")
    private Long executionTimeMs;

    @Schema(description = "扩展字段")
    private Map<String, Object> extraData;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

