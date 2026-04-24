package io.vanguard.testops.api.dto.converter;

import io.vanguard.testops.api.domain.ApiScenarioCsv;
import io.vanguard.testops.api.dto.definition.ApiTestCaseDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioImportDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口导入数据准备结果
 */
@Data
public class ApiScenarioImportParseResult {
    @Schema(description = "导入的场景")
    List<ApiScenarioImportDetail> importScenarioList = new ArrayList<>();

    @Schema(description = "有关联关系的场景")
    List<ApiScenarioImportDetail> relatedScenarioList = new ArrayList<>();

    @Schema(description = "场景CSV相关的数据")
    private List<ApiScenarioCsv> apiScenarioCsvList = new ArrayList<>();


    @Schema(description = "所有场景步骤内容")
    private Map<String, String> scenarioStepBlobMap = new HashMap<>();


    @Schema(description = "有关联的接口定义")
    private List<ApiDefinitionDetail> relatedApiDefinitions = new ArrayList<>();

    @Schema(description = "有关联的接口用例")
    private List<ApiTestCaseDTO> relatedApiTestCaseList = new ArrayList<>();

}
