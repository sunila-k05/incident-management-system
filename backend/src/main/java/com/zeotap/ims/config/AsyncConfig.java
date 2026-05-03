package com.zeotap.ims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "signalExecutor")
    public Executor signalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core threads always alive
        executor.setCorePoolSize(4);
        // Max threads under heavy load
        executor.setMaxPoolSize(10);
        // Queue for pending tasks
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("signal-worker-");
        executor.initialize();
        return executor;
    }
}
