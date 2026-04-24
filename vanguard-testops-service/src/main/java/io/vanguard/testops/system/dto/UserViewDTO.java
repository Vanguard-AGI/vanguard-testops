package io.vanguard.testops.system.dto;

import io.vanguard.testops.system.domain.UserView;
import io.vanguard.testops.sdk.dto.CombineCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-09-02  10:15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserViewDTO extends UserView {
    @Schema(description = "筛选条件")
    private List<CombineCondition> conditions;
}
