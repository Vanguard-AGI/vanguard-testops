package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 插件同步响应 DTO
 */
@Data
public class PluginSyncResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "成功同步的节点数量")
    private Integer successCount;

    @Schema(description = "失败的节点数量")
    private Integer failCount;

    @Schema(description = "失败的节点详情")
    private List<FailItem> failItems;

    @Data
    public static class FailItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "节点索引")
        private Integer index;

        @Schema(description = "失败原因")
        private String reason;
    }
}

