package io.vanguard.testops.system.dto.user;

import io.vanguard.testops.system.dto.sdk.ExcludeOptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加载选项时，标记是否已经关联过，前端需要 exclude 判断禁止重复关联
 * @author Jan
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserExcludeOptionDTO extends ExcludeOptionDTO {
    @Schema(description =  "邮箱")
    private String email;
}
