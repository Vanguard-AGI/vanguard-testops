package io.vanguard.testops.plan.runtime.config;

import io.vanguard.testops.sdk.util.LogUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 测试计划指标计算异步配置
 * 用于异步执行耗时的测试计划指标计算任务，避免阻塞主线程
 */
@Configuration
@EnableAsync(proxyTargetClass = true)
public class TestPlanMetricsAsyncConfig {

    /**
     * 指标计算专用线程池
     * 核心线程数: 2 (同时计算2个任务)
     * 最大线程数: 5 (高峰期最多5个并发)
     * 队列容量: 100 (最多缓存100个待执行任务)
     */
    @Bean(name = "meticsCalculationExecutor")
    public Executor meticsCalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数
        executor.setMaxPoolSize(5);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("metrics-calc-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        LogUtils.info("指标计算线程池初始化完成: corePoolSize=2, maxPoolSize=5, queueCapacity=100");
        
        return executor;
    }
}
