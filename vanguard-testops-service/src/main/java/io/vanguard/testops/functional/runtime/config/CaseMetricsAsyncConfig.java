package io.vanguard.testops.functional.runtime.config;

import io.vanguard.testops.sdk.util.LogUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 用例指标计算异步配置
 * 用于异步执行耗时的指标计算任务（CS计算、需求同步等），避免阻塞主线程
 */
@Configuration
@EnableAsync(proxyTargetClass = true)
public class CaseMetricsAsyncConfig {

    /**
     * CS计算专用线程池
     * 核心线程数: 2 (同时计算2个任务)
     * 最大线程数: 5 (高峰期最多5个并发)
     * 队列容量: 100 (最多缓存100个待执行任务)
     */
    @Bean(name = "csCalculationExecutor")
    public Executor csCalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数
        executor.setMaxPoolSize(5);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("cs-calc-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        LogUtils.info("CS计算线程池初始化完成: corePoolSize=2, maxPoolSize=5, queueCapacity=100");
        
        return executor;
    }
    
    /**
     * 飞书需求同步专用线程池
     * 核心线程数: 1 (需求同步通常是单任务)
     * 最大线程数: 2 (最多2个并发同步任务)
     * 队列容量: 10 (需求同步任务较少)
     */
    @Bean(name = "feishuSyncExecutor")
    public Executor feishuSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(1);
        
        // 最大线程数
        executor.setMaxPoolSize(2);
        
        // 队列容量
        executor.setQueueCapacity(10);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("feishu-sync-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        LogUtils.info("飞书同步线程池初始化完成: corePoolSize=1, maxPoolSize=2, queueCapacity=10");
        
        return executor;
    }
}
