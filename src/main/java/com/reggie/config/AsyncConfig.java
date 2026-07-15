package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * 异步任务线程池配置
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 缓存双删专用线程池
     * <p>
     * 核心线程 2：双删是低频操作，无需大量线程
     * 最大线程 5：突发高并发时弹性扩容
     * 队列容量 200：缓冲等待，超过则走拒绝策略
     * 拒绝策略：CallerRunsPolicy（由调用线程执行，不丢任务）
     */
    @Bean("cacheDoubleDeleteExecutor")
    public ThreadPoolTaskExecutor cacheDoubleDeleteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("cache-double-del-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 传递ThreadLocal上下文，确保异步线程能获取BaseContext中的userId和tenantId
        executor.setTaskDecorator(new org.springframework.core.task.TaskDecorator() {
            @Override
            public Runnable decorate(Runnable runnable) {
                Long currentId = com.reggie.common.BaseContext.getCurrentId();
                Long currentTenantId = com.reggie.common.BaseContext.getCurrentTenantId();
                return () -> {
                    try {
                        if (currentId != null) {
                            com.reggie.common.BaseContext.setCurrentId(currentId);
                        }
                        if (currentTenantId != null) {
                            com.reggie.common.BaseContext.setCurrentTenantId(currentTenantId);
                        }
                        runnable.run();
                    } finally {
                        com.reggie.common.BaseContext.remove();
                    }
                };
            }
        });
        executor.initialize();
        log.info("[线程池] 缓存双删线程池初始化完成：core=2, max=5, queue=200");
        return executor;
    }
}
