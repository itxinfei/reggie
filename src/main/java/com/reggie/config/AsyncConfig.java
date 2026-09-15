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
            /**
             * 处理 decorate。
             * @param runnable 参数 runnable
             * @return 返回结果
             */
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

    /**
     * 推荐引擎专用线程池
     * <p>
     * 用于推荐结果异步缓存、用户偏好异步分析等任务。替代原 RecommendServiceImpl 中
     * 静态 FixedThreadPool(4)（永不关闭且 4 线程成瓶颈），由 Spring 管理生命周期，
     * 应用关闭时优雅停机。
     * </p>
     * <p>
     * 核心线程 4：与原线程数对齐，保证基础吞吐
     * 最大线程 16：突发高并发时弹性扩容
     * 队列容量 500：推荐任务可容忍延迟，缓冲排队
     * 拒绝策略：CallerRunsPolicy（由调用线程执行，不丢任务）
     * </p>
     */
    @Bean("recommendExecutor")
    public ThreadPoolTaskExecutor recommendExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("recommend-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 传递ThreadLocal上下文，确保异步线程能获取BaseContext中的userId和tenantId
        executor.setTaskDecorator(runnable -> {
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
        });
        executor.initialize();
        log.info("[线程池] 推荐引擎异步线程池初始化完成：core=4, max=16, queue=500");
        return executor;
    }

    /**
     * AI 健康检查探活线程池
     * <p>
     * 用途：AI 健康探活（/api/ai/health）需要在「有界时间」内拿到结果，
     * 不能让 HTTP 请求线程直接等待外部 AI 接口的完整超时（供应商配置 30s，Ollama 60s）。
     * 该线程池配合 Future.get(timeout) 使用，超时后调用方立即返回「不可用」。
     * <p>
     * 拒绝策略必须为 AbortPolicy：若误用 CallerRunsPolicy，任务会回退到请求线程执行，
     * 探活将重新变成同步阻塞，失去超时保护的意义。
     */
    @Bean("aiHealthProbeExecutor")
    public ThreadPoolTaskExecutor aiHealthProbeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-health-probe-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        // 传递ThreadLocal上下文，确保探活线程能获取BaseContext中的userId和tenantId
        executor.setTaskDecorator(runnable -> {
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
        });
        executor.initialize();
        log.info("[线程池] AI健康探活线程池初始化完成：core=2, max=4, queue=50");
        return executor;
    }
}
