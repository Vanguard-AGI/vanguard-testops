package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.functional.domain.CaseReviewFunctionalCaseArchive;
import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCaseBlob;
import io.vanguard.testops.sdk.util.CompressUtils;
import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class FunctionalCaseInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> functionalCaseCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList.add(new MybatisInterceptorConfig(CaseReviewFunctionalCaseArchive.class, "content", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(FunctionalCaseBlob.class, "steps", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(FunctionalCaseBlob.class, "textDescription", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(FunctionalCaseBlob.class, "expectedResult", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(FunctionalCaseBlob.class, "prerequisite", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(FunctionalCaseBlob.class, "description", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(CaseReviewHistory.class, "content", CompressUtils.class, "zip", "unzip"));

        return configList;
    }
}
