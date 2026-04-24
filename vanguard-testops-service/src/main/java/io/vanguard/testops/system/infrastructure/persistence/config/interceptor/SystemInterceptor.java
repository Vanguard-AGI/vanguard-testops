package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.sdk.util.CompressUtils;
import io.vanguard.testops.system.domain.*;
import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SystemInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> systemCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList.add(new MybatisInterceptorConfig(TestResourcePoolBlob.class, "configuration", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(AuthSource.class, "configuration", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(NoviceStatistics.class, "dataOption", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(ServiceIntegration.class, "configuration", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(UserExtend.class, "platformInfo", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(PluginScript.class, "script", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(PlatformSource.class, "config", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(UserLayout.class, "configuration", CompressUtils.class, "zip", "unzip"));
        return configList;
    }
}
