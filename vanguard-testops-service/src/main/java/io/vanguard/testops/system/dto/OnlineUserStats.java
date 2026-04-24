package io.vanguard.testops.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 在线用户统计：数量 + 明细（含在线时长）
 */
@Data
@Schema(description = "在线用户统计")
public class OnlineUserStats {

    @Schema(description = "在线用户数")
    private Integer count;

    @Schema(description = "在线用户明细（含会话数、在线时长）")
    private List<OnlineUserDetail> details;
}
