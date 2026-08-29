package com.skillcircle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables asynchronous execution for fire-and-forget AI work (e.g. thread
 * auto-summarization) so slow LLM calls never block the request / WebSocket thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Small bounded executor for background AI tasks.
     *
     * <p>Kept intentionally small so a burst of chat activity cannot spawn
     * unbounded summarization work. When the queue is full, excess tasks run on
     * the caller thread ({@link ThreadPoolExecutor.CallerRunsPolicy}) rather than
     * being silently dropped.
     */
    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
