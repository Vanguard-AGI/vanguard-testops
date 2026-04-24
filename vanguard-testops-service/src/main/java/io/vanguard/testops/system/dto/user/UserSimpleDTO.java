package io.vanguard.testops.system.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户简单信息DTO（用于公开接口）
 */
@Data
public class UserSimpleDTO {
    
    @Schema(description = "用户ID")
    private String id;
    
    @Schema(description = "用户名称")
    private String name;
    
    @Schema(description = "用户邮箱")
    private String email;
}

