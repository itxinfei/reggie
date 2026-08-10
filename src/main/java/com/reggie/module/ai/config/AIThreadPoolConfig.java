package com.reggie.module.ai.config;

import com.reggie.common.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * AI模块异步线程池配置，为AI对话、画像刷新等异步任务提供专用线程池。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Slf4j
@Configuration
@EnableAsync
public class AIThreadPoolConfig {

    @Bean("aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-chat-");
        // 修改点：任务满时由调用线程执行，防止丢失任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 传递ThreadLocal上下文，确保异步线程能获取BaseContext中的userId和tenantId
        // 修改点：AI对话在异步线程中执行多租户查询（如历史消息加载），必须传播租户上下文以保证租户隔离生效
        executor.setTaskDecorator(new TaskDecorator() {
            @Override
            public Runnable decorate(Runnable runnable) {
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
            }
        });
        executor.initialize();
        log.info("AI异步线程池已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                4, 8, 100);
        return executor;
    }
}