package io.vanguard.testops.api.dto.definition;

import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class EnvApiTreeDTO {

    @Schema(description = "模块树")
    private List<BaseTreeNode> moduleTree;
    @Schema(description = "选中的id")
    private List<ApiModuleDTO> selectedModules;
}
