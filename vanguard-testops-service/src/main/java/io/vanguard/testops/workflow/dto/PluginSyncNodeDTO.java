package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 插件同步节点 DTO
 * 用于返回给前端展示
 */
@Data
public class PluginSyncNodeDTO {

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "节点类型: HTTP/SQL")
    private String nodeType;

    @Schema(description = "节点数据（JSON格式）")
    private Map<String, Object> endpointData;

    @Schema(description = "创建时间（毫秒）")
    private Long createTime;

    @Schema(description = "更新时间（毫秒）")
    private Long updateTime;
}

