package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
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

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "scheduled-task-" + THREAD_COUNTER.incrementAndGet());
            t.setUncaughtExceptionHandler((thread, ex) ->
                log.error("[定时任务] 线程异常终止: {}", thread.getName(), ex)
            );
            return t;
        }));
    }
}
