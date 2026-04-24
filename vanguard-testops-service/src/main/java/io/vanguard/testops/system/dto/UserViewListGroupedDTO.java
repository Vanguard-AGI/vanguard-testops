package io.vanguard.testops.system.dto;

import io.vanguard.testops.system.domain.UserView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-09-02  10:15
 */
@Data
public class UserViewListGroupedDTO {
    @Schema(description = "系统视图")
    private List<UserView> internalViews;
    @Schema(description = "自定义视图")
    private List<UserView> customViews;
}
