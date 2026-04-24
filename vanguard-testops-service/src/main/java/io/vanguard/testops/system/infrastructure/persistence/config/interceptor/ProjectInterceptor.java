package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.project.domain.CustomFunctionBlob;
import io.vanguard.testops.project.domain.FileModuleRepository;
import io.vanguard.testops.sdk.domain.EnvironmentBlob;
import io.vanguard.testops.sdk.util.CompressUtils;
import io.vanguard.testops.sdk.util.EncryptUtils;
import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ProjectInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> projectCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList.add(new MybatisInterceptorConfig(FileModuleRepository.class, "token", EncryptUtils.class, "aesEncrypt", "aesDecrypt"));
        configList.add(new MybatisInterceptorConfig(CustomFunctionBlob.class, "script", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(CustomFunctionBlob.class, "result", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(CustomFunctionBlob.class, "params", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(EnvironmentBlob.class, "config", CompressUtils.class, "zip", "unzip"));

        return configList;
    }
}
