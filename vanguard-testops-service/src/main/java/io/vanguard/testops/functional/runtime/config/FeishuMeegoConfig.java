package io.vanguard.testops.functional.runtime.config;

import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 飞书Meego集成配置类
 * 注意：@EnableScheduling 已在 ScheduleConfig 中全局启用，此处不需要重复启用
 */
@Configuration
@PropertySource(value = "classpath:application-metrics.properties", ignoreResourceNotFound = true)
public class FeishuMeegoConfig {

    @Value("${metrics.schedule.case-cs:0 0 2 * * ?}")
    private String caseCsCron;
    
    @Value("${metrics.schedule.case-metrics-daily:0 0 4 * * ?}")
    private String caseMetricsDailyCron;
    
    @Value("${metrics.schedule.case-metrics-weekly:0 0 4 ? * MON}")
    private String caseMetricsWeeklyCron;
    
    @Value("${metrics.schedule.case-metrics-monthly:0 0 4 1 * ?}")
    private String caseMetricsMonthlyCron;
    
    @Value("${metrics.schedule.feishu-sync:0 0 1 * * ?}")
    private String feishuSyncCron;

    @PostConstruct
    public void init() {
        LogUtils.info("========== 定时任务配置已加载 ==========");
        LogUtils.info("用例CS值计算: {}", caseCsCron);
        LogUtils.info("每日效能指标计算: {}", caseMetricsDailyCron);
        LogUtils.info("每周效能指标计算: {}", caseMetricsWeeklyCron);
        LogUtils.info("每月效能指标计算: {}", caseMetricsMonthlyCron);
        LogUtils.info("飞书需求同步: {}", feishuSyncCron);
        LogUtils.info("========================================");
    }

    /**
     * 创建RestTemplate Bean用于HTTP请求
     */
    @Bean("feishuRestTemplate")
    public RestTemplate feishuRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 连接超时10秒
        factory.setReadTimeout(30000);     // 读取超时30秒
        return new RestTemplate(factory);
    }
}
