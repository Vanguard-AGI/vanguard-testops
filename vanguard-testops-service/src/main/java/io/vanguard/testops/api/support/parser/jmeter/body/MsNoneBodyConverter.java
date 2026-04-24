package io.vanguard.testops.api.support.parser.jmeter.body;

import io.vanguard.testops.api.dto.request.http.body.NoneBody;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-14  21:15
 */
public class MsNoneBodyConverter extends MsBodyConverter<NoneBody> {
    @Override
    public String parse(HTTPSamplerProxy sampler, NoneBody body, ParameterConfig config) {
        // do nothing
        return null;
    }
}
