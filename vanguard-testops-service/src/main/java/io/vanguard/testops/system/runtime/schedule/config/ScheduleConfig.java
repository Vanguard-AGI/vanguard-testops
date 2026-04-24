package io.vanguard.testops.system.runtime.schedule.config;

import io.vanguard.testops.system.runtime.schedule.ScheduleManager;
import io.vanguard.testops.system.runtime.schedule.ScheduleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Bean
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public ScheduleManager scheduleManager() {
        return new ScheduleManager();
    }

    @Bean
    public ScheduleService scheduleService() {
        return new ScheduleService();
    }
}
