package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.sdk.domain.OperationLogBlob;
import io.vanguard.testops.sdk.domain.ShareInfo;
import io.vanguard.testops.sdk.util.CompressUtils;
import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SdkInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> sdkCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList.add(new MybatisInterceptorConfig(OperationLogBlob.class, "originalValue", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(OperationLogBlob.class, "modifiedValue", CompressUtils.class, "zip", "unzip"));
        // ShareInfo
        configList.add(new MybatisInterceptorConfig(ShareInfo.class, "customData", CompressUtils.class, "zipString", "unzipString"));

        return configList;
    }
}
