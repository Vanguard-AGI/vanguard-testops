package io.vanguard.testops.api.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class ShareInfoDTO {
    @Schema(description = "分享id")
    private String id;
    @Schema(description = "分享链接")
    private String shareUrl;
}
