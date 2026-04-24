package io.vanguard.testops.api.support.parser.step;

import io.vanguard.testops.api.domain.ApiTestCaseBlob;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepDetailRequest;
import io.vanguard.testops.api.mapper.ApiTestCaseBlobMapper;
import io.vanguard.testops.api.support.data.ApiDataUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: Jan
 * @CreateTime: 2024-01-20  15:43
 */
public class ApiCaseStepParser extends StepParser {
    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        if (isRef(step.getRefType())) {
            return StringUtils.isBlank(resourceBlob) ? null : parse2MsTestElement(resourceBlob);
        } else {
            if (StringUtils.isBlank(stepDetail)) {
                return null;
            }
            return StringUtils.isBlank(stepDetail) ? null : ApiDataUtils.parseObject(stepDetail, AbstractMsTestElement.class);
        }
    }

    @Override
    public Object parseDetail(ApiScenarioStepDetailRequest step) {
        if (isRef(step.getRefType())) {
            ApiTestCaseBlobMapper apiTestCaseBlobMapper = CommonBeanFactory.getBean(ApiTestCaseBlobMapper.class);
            ApiTestCaseBlob apiTestCaseBlob = apiTestCaseBlobMapper.selectByPrimaryKey(step.getResourceId());
            if (apiTestCaseBlob == null) {
                return null;
            }
            return parse2MsTestElement(new String(apiTestCaseBlob.getRequest()));
        } else {
            return parse2MsTestElement(getStepBlobString(step.getId()));
        }
    }
}
