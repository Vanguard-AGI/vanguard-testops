package io.vanguard.testops.requirementquality.dto;

import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 需求质量看板 - 筛选项（项目、需求列表、状态）
 */
@Data
public class RequirementQualityFilterOptionsDTO {

    @Schema(description = "项目选项（仅包含有关联测试计划组的需求的项目）")
    private List<OptionDTO> projectOptions;

    @Schema(description = "需求选项（仅已关联测试计划组的需求）")
    private List<OptionDTO> requirementOptions;

    @Schema(description = "状态选项（测试计划组状态：未开始/进行中/已完成）")
    private List<OptionDTO> statusOptions;
}
