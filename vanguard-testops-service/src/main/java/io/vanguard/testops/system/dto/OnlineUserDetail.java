package io.vanguard.testops.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在线用户明细：单用户会话数、在线时长
 */
@Data
@Schema(description = "在线用户明细")
public class OnlineUserDetail {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String name;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "当前会话数")
    private Integer sessionCount;

    @Schema(description = "在线时长（秒），按最近一次登录的会话创建时间(creationTime)起算至当前")
    private Long onlineDurationSeconds;

    @Schema(description = "会话创建时间（毫秒时间戳），多会话取最近一次登录的会话创建时间")
    private Long creationTime;
}
