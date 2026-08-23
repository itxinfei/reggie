package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 定时任务调度器配置
 * 使用多线程池执行 @Scheduled 任务，避免单个任务阻塞其他任务。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
@Slf4j
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

    /**
     * 定时任务线程池引用，用于应用关闭时优雅停止
     */
    private ScheduledExecutorService scheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "scheduled-task-" + THREAD_COUNTER.incrementAndGet());
            t.setUncaughtExceptionHandler((thread, ex) ->
                log.error("[定时任务] 线程异常终止: {}", thread.getName(), ex)
            );
            return t;
        });
        taskRegistrar.setScheduler(this.scheduler);
    }

    /**
     * 应用关闭时优雅关闭线程池，避免非守护线程阻止 JVM 退出
     */
    @PreDestroy
    public void destroy() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[定时任务] 线程池在30秒内未正常关闭，执行强制关闭");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[定时任务] 线程池已关闭");
    }
}
