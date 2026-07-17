package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * 事件监听器异步线程池配置，为领域事件监听器提供专用线程池。
 * </p>
 * <p>
 * 与 AI 模块线程池隔离，避免事件处理阻塞 AI 对话任务。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
@Slf4j
@Configuration
public class EventListenerThreadPoolConfig {

    @Bean("eventListenerExecutor")
    public Executor eventListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("event-listener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 传递ThreadLocal上下文，确保事件监听器能获取BaseContext中的userId和tenantId
        executor.setTaskDecorator(runnable -> {
            Long currentId = BaseContext.getCurrentId();
            Long currentTenantId = BaseContext.getCurrentTenantId();
            return () -> {
                try {
                    if (currentId != null) {
                        BaseContext.setCurrentId(currentId);
                    }
                    if (currentTenantId != null) {
                        BaseContext.setCurrentTenantId(currentTenantId);
                    }
                    runnable.run();
                } finally {
                    BaseContext.remove();
                }
            };
        });
        executor.initialize();
        log.info("[线程池] 事件监听器线程池初始化完成：core=2, max=5, queue=100");
        return executor;
    }
}
