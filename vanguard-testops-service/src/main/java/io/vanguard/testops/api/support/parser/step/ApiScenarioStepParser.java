package io.vanguard.testops.api.support.parser.step;

import io.vanguard.testops.api.domain.ApiScenarioBlob;
import io.vanguard.testops.api.dto.request.MsScenario;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepDetailRequest;
import io.vanguard.testops.api.dto.scenario.ScenarioConfig;
import io.vanguard.testops.api.mapper.ApiScenarioBlobMapper;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.JSON;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: Jan
 * @CreateTime: 2024-01-20  15:43
 */
public class ApiScenarioStepParser extends StepParser {
    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        MsScenario msScenario = new MsScenario();
        msScenario.setRefType(step.getRefType());
        if (isRef(step.getRefType())) {
            if (StringUtils.isNotBlank(resourceBlob)) {
                msScenario.setScenarioConfig(JSON.parseObject(resourceBlob, ScenarioConfig.class));
            }
        } else {
            if (StringUtils.isNotBlank(stepDetail)) {
                msScenario.setScenarioConfig(JSON.parseObject(stepDetail, ScenarioConfig.class));
            }
        }
        return msScenario;
    }

    /**
     * 获取场景配置详情
     * @param step
     * @return
     */
    @Override
    public Object parseDetail(ApiScenarioStepDetailRequest step) {
        if (isRef(step.getRefType())) {
            ApiScenarioBlobMapper apiScenarioBlobMapper = CommonBeanFactory.getBean(ApiScenarioBlobMapper.class);
            ApiScenarioBlob apiScenarioBlob = apiScenarioBlobMapper.selectByPrimaryKey(step.getResourceId());
            if (apiScenarioBlob == null || apiScenarioBlob.getConfig() == null) {
                return null;
            }
            return JSON.parseObject(new String(apiScenarioBlob.getConfig()), ScenarioConfig.class);
        } else {
            String stepDetailStr= getStepBlobString(step.getId());
            if (StringUtils.isBlank(stepDetailStr)) {
                return null;
            }
            return JSON.parseObject(stepDetailStr, ScenarioConfig.class);
        }
    }
}
