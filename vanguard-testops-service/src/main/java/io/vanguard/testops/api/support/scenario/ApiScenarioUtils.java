package io.vanguard.testops.api.support.scenario;

import io.vanguard.testops.api.domain.ApiScenario;
import io.vanguard.testops.api.domain.ApiScenarioBlob;
import io.vanguard.testops.api.dto.scenario.ApiScenarioDetail;
import io.vanguard.testops.api.dto.scenario.ScenarioConfig;
import io.vanguard.testops.sdk.constants.ModuleConstants;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiScenarioUtils {
    public static List<ApiScenarioDetail> parseApiScenarioDetail(List<ApiScenario> apiScenarios, Map<String, ApiScenarioBlob> scenarioBlobMap, Map<String, String> moduleMap) {
        List<ApiScenarioDetail> returnList = new ArrayList<>();
        for (ApiScenario apiScenario : apiScenarios) {
            ApiScenarioDetail apiScenarioDetail = BeanUtils.copyBean(new ApiScenarioDetail(), apiScenario);
            apiScenarioDetail.setSteps(List.of());
            if (moduleMap.containsKey(apiScenarioDetail.getModuleId())) {
                apiScenarioDetail.setModulePath(moduleMap.get(apiScenarioDetail.getModuleId()));
            } else {
                apiScenarioDetail.setModuleId(ModuleConstants.DEFAULT_NODE_ID);
                apiScenarioDetail.setModulePath(Translator.get("api_unplanned_scenario"));
            }
            ApiScenarioBlob apiScenarioBlob = scenarioBlobMap.get(apiScenario.getId());
            if (apiScenarioBlob != null) {
                apiScenarioDetail.setScenarioConfig(JSON.parseObject(new String(apiScenarioBlob.getConfig()), ScenarioConfig.class));
            }
            returnList.add(apiScenarioDetail);
        }
        return returnList;
    }
}
