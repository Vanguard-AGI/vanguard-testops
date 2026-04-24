package io.vanguard.testops.system.infrastructure.persistence.config.interceptor;

import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.vanguard.testops.plan.domain.TestPlanReportSummary;
import io.vanguard.testops.sdk.util.CompressUtils;
import io.vanguard.testops.system.infrastructure.persistence.config.interceptor.MybatisInterceptorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TestPlanInterceptor {
    @Bean
    public List<MybatisInterceptorConfig> tstPlanCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList.add(new MybatisInterceptorConfig(TestPlanCaseExecuteHistory.class, "content", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(TestPlanCaseExecuteHistory.class, "steps", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(TestPlanReportSummary.class, "functionalExecuteResult", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(TestPlanReportSummary.class, "apiExecuteResult", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(TestPlanReportSummary.class, "scenarioExecuteResult", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(TestPlanReportSummary.class, "executeResult", CompressUtils.class, "zip", "unzip"));

        return configList;
    }
}
