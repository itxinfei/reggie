package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存双删工具类
 * <p>
 * 【缓存双删策略】
 * 解决高并发场景下缓存与数据库不一致问题：
 * 1. 写操作前先删除缓存（一删）
 * 2. 执行数据库写操作
 * 3. 短暂延时后再次删除缓存（二删），防止并发读写入旧数据
 * <p>
 * 修改点：使用 ScheduledExecutorService 替代 @Async + Thread.sleep，避免阻塞线程池
 * <p>
 * 使用方式：
 * <pre>{@code
 *   // 精确删除
 *   redisCacheUtil.doubleDelete("setmeal", id);
 *
 *   // 全量清空
 *   redisCacheUtil.doubleDeleteAllEntries("dishes");
 * }</pre>
 *
 * @author Reggie Team
 */
@Slf4j
@Component
public class RedisCacheUtil {

    /**
     * 二删延时（毫秒），建议 500-1000ms
     * 太短：来不及拦截并发脏写入
     * 太长：缓存窗口期过长，影响用户体验
     */
    private static final long SECOND_DELETE_DELAY_MS = 600;

    /**
     * 二删延时调度器
     * 修改点：使用 ScheduledExecutorService 替代 @Async + Thread.sleep
     */
    private volatile ScheduledExecutorService scheduledExecutor;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        if (redisTemplate == null) {
            log.warn("[缓存双删] Redis 不可用，缓存功能已禁用");
            return;
        }
        // 修改点：单线程调度器，按序执行延时删除，不阻塞业务线程池
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-double-del-scheduled");
            t.setDaemon(true);
            return t;
        });
        log.info("[缓存双删] 延时调度器初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 双删：针对指定缓存名 + Key 的精确删除
     * <p>
     * 注意：Spring Cache 实际存储的 Key 格式为 "缓存名::Key值"
     * 例如 @Cacheable(value = "setmeal", key = "#id") → "setmeal::123"
     *
     * @param cacheName 缓存名称（对应 @Cacheable 的 value）
     * @param key       缓存的 Key 值
     */
    public void doubleDelete(String cacheName, Object key) {
        if (redisTemplate == null) return;

        // Spring Cache 默认 Key 格式: "cacheName::keyValue"
        String redisKey = cacheKey(cacheName, key);

        // 一删：先清除旧缓存
        deleteKey(redisKey);
        log.debug("[缓存双删] 一删完成：{}", redisKey);

        // 二删：延时执行，不阻塞主流程
        scheduleSecondDelete(redisKey);
    }

    /**
     * 双删：清空整个缓存域（对应 @CacheEvict(allEntries = true)）
     * <p>
     * 注意：Spring Cache 的 allEntries 实际上是通过 Redis 的 keys + del 实现
     * 这里同样使用模式匹配批量删除
     *
     * @param cacheName 缓存名称
     */
    public void doubleDeleteAllEntries(String cacheName) {
        if (redisTemplate == null) return;

        String pattern = cacheKeyPattern(cacheName);

        // 一删：清除所有匹配的 Key
        int count = deleteByPattern(pattern);
        log.debug("[缓存双删] 一删完成：{}（{} 条）", cacheName, count);

        // 二删：延时删除
        scheduleSecondDeleteByPattern(pattern, cacheName);
    }

    /**
     * 同步删除单个 Key
     */
    public void deleteKey(String redisKey) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("[缓存] 删除 Key 失败：{}", redisKey, e);
        }
    }

    /**
     * 同步按模式批量删除（使用SCAN命令，避免阻塞Redis）
     * <p>
     * 修改点：直接使用 redisTemplate.delete(keys) 批量删除，简化分批逻辑
     *
     * @return 删除的 Key 数量
     */
    public int deleteByPattern(String pattern) {
        if (redisTemplate == null) return 0;
        try {
            // 使用RedisCallback直接操作Connection，支持SCAN
            Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> result = new HashSet<>();
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                Cursor<byte[]> cursor = connection.scan(options);
                try {
                    while (cursor.hasNext()) {
                        result.add(new String(cursor.next()));
                    }
                } finally {
                    try {
                        cursor.close();
                    } catch (Exception ignored) {
                        // 忽略关闭异常
                    }
                }
                return result;
            });

            if (keys != null && !keys.isEmpty()) {
                // 直接批量删除，RedisTemplate.delete(Set) 已支持批量操作
                Long deleted = redisTemplate.delete(keys);
                log.debug("[缓存] 批量删除完成：pattern={}, count={}", pattern, deleted);
                return deleted != null ? deleted.intValue() : 0;
            }
        } catch (Exception e) {
            log.warn("[缓存] 批量删除失败：{}", pattern, e);
        }
        return 0;
    }

    /**
     * 使用 ScheduledExecutorService 延时执行二删（单个 Key）
     * 修改点：替代 @Async + Thread.sleep，不阻塞线程池
     */
    private void scheduleSecondDelete(String redisKey) {
        scheduledExecutor.schedule(() -> {
            try {
                redisTemplate.delete(redisKey);
                log.debug("[缓存双删] 二删完成：{}", redisKey);
            } catch (Exception e) {
                log.warn("[缓存双删] 二删失败：{}", redisKey, e);
            }
        }, SECOND_DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 使用 ScheduledExecutorService 延时执行二删（按模式批量）
     * 修改点：替代 @Async + Thread.sleep
     */
    private void scheduleSecondDeleteByPattern(String pattern, String cacheName) {
        scheduledExecutor.schedule(() -> {
            try {
                int count = deleteByPattern(pattern);
                log.debug("[缓存双删] 二删完成：{}（{} 条）", cacheName, count);
            } catch (Exception e) {
                log.warn("[缓存双删] 二删失败：{}, cacheName={}", pattern, cacheName, e);
            }
        }, SECOND_DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 构建缓存Key
     * Spring Cache默认格式：cacheName::keyValue
     *
     * @param cacheName 缓存名称
     * @param key       缓存Key
     * @return 完整的Redis Key
     */
    public static String cacheKey(String cacheName, Object key) {
        return cacheName + "::" + key;
    }

    /**
     * 构建缓存Key匹配模式
     *
     * @param cacheName 缓存名称
     * @return 匹配模式
     */
    public static String cacheKeyPattern(String cacheName) {
        return cacheName + "::*";
    }
}