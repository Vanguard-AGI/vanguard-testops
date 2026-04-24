package io.vanguard.testops.system.runtime.schedule.config;

import io.vanguard.testops.sdk.util.LogUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

/**
 * Spring @Scheduled 定时任务配置
 * 配置多线程 TaskScheduler，解决默认单线程调度器的问题：
 * 1. 默认单线程调度器，任一任务阻塞会导致所有任务延迟
 * 2. 任一任务抛出未捕获异常会导致整个调度器停止
 */
@Configuration
public class ScheduledTaskConfig {

    /**
     * 配置多线程 TaskScheduler
     * - 线程池大小：10（足够支持多个定时任务并行执行）
     * - 线程名前缀：scheduled-task-
     * - 自定义错误处理器：记录错误日志但不中断调度器
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        
        // 自定义错误处理器：记录错误但不中断调度器
        scheduler.setErrorHandler(new ScheduledTaskErrorHandler());
        
        return scheduler;
    }

    /**
     * 定时任务错误处理器
     * 捕获所有未处理的异常，记录日志但不中断调度器
     */
    private static class ScheduledTaskErrorHandler implements ErrorHandler {
        @Override
        public void handleError(Throwable t) {
            LogUtils.error("========== 定时任务执行异常 ==========");
            LogUtils.error("异常类型: " + t.getClass().getName());
            LogUtils.error("异常消息: " + t.getMessage());
            LogUtils.error("堆栈信息: ", t);
            LogUtils.error("========== 定时任务将继续执行 ==========");
            // 不抛出异常，让调度器继续工作
        }
    }
}
