package io.vanguard.testops.system.dto.request.user;

import io.vanguard.testops.sdk.util.RsaKey;
import io.vanguard.testops.sdk.util.RsaUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonalUpdatePasswordRequest {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.id.not_blank}")
    private String id;

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.password.not.blank}")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.password.not.blank}")
    private String newPassword;

    public String getOldPassword() {
        try {
            RsaKey rsaKey = RsaUtils.getRsaKey();
            return RsaUtils.privateDecrypt(oldPassword, rsaKey.getPrivateKey());
        } catch (Exception e) {
            return oldPassword;
        }
    }

    public String getNewPassword() {
        try {
            RsaKey rsaKey = RsaUtils.getRsaKey();
            return RsaUtils.privateDecrypt(newPassword, rsaKey.getPrivateKey());
        } catch (Exception e) {
            return newPassword;
        }
    }
}
