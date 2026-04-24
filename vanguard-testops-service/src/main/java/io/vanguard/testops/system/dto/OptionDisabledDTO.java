package io.vanguard.testops.system.dto;

import io.vanguard.testops.system.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionDisabledDTO extends User {
    @Schema(description =  "是否已经关联过")
    private Boolean disabled = false;
}
