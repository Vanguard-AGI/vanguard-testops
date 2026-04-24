package io.vanguard.testops.api.support.parser.jmeter.body;

import io.vanguard.testops.api.dto.request.http.body.WWWFormBody;
import io.vanguard.testops.api.dto.request.http.body.WWWFormKV;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-14  20:34
 */
public class MsWWWFormBodyConverter extends MsBodyConverter<WWWFormBody> {
    @Override
    public String parse(HTTPSamplerProxy sampler, WWWFormBody body, ParameterConfig config) {
        List<WWWFormKV> formValues = body.getFormValues();
        List<WWWFormKV> validFormValues = formValues.stream()
                .filter(WWWFormKV::getEnable)
                .filter(WWWFormKV::isValid)
                .collect(Collectors.toList());
        sampler.setArguments(getArguments(validFormValues));
        return MediaType.APPLICATION_FORM_URLENCODED_VALUE;
    }
}
