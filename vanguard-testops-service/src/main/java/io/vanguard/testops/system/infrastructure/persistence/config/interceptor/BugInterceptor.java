package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class BugInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> bugCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();
        return configList;
    }
}
