package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis 缓存双删工具类
 * <p>
 * 【缓存双删策略】
 * 解决高并发场景下缓存与数据库不一致问题：
 * 1. 写操作前先删除缓存（一删）
 * 2. 执行数据库写操作
 * 3. 短暂延时后再次删除缓存（二删），防止并发读写入旧数据
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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
        // Spring Cache 默认 Key 格式: "cacheName::keyValue"
        String redisKey = cacheName + "::" + key;

        // 一删：先清除旧缓存
        deleteKey(redisKey);
        log.debug("[缓存双删] 一删完成：{}", redisKey);

        // 二删：异步延时删除，防止并发读写入脏数据
        asyncSecondDelete(redisKey);
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
        String pattern = cacheName + "::*";

        // 一删：清除所有匹配的 Key
        int count = deleteByPattern(pattern);
        log.debug("[缓存双删] 一删完成：{}（{} 条）", cacheName, count);

        // 二删：异步延时删除
        asyncSecondDeleteByPattern(pattern, cacheName);
    }

    /**
     * 同步删除单个 Key
     */
    public void deleteKey(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("[缓存] 删除 Key 失败：{}", redisKey, e);
        }
    }

    /**
     * 同步按模式批量删除（使用SCAN命令，避免阻塞Redis）
     *
     * @return 删除的 Key 数量
     */
    public int deleteByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            // 使用SCAN命令替代KEYS命令，避免在大数据量下阻塞Redis
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                // 分批删除，每批最多100个Key
                int deleted = 0;
                int batchSize = 100;
                for (int i = 0; i < keys.size(); i += batchSize) {
                    Set<String> batch = new HashSet<>(
                        keys.stream().skip(i).limit(batchSize).collect(java.util.stream.Collectors.toSet())
                    );
                    redisTemplate.delete(batch);
                    deleted += batch.size();
                }
                return deleted;
            }
        } catch (Exception e) {
            log.warn("[缓存] 批量删除失败：{}", pattern, e);
        }
        return 0;
    }

    /**
     * 异步执行二删（单个 Key）
     * <p>
     * 使用独立线程池执行，不阻塞主业务流程
     */
    @Async("cacheDoubleDeleteExecutor")
    public void asyncSecondDelete(String redisKey) {
        try {
            Thread.sleep(SECOND_DELETE_DELAY_MS);
            redisTemplate.delete(redisKey);
            log.debug("[缓存双删] 二删完成：{}", redisKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[缓存双删] 二删被中断：{}", redisKey);
        } catch (Exception e) {
            log.warn("[缓存双删] 二删失败：{}", redisKey, e);
        }
    }

    /**
     * 异步执行二删（按模式批量删除）
     */
    @Async("cacheDoubleDeleteExecutor")
    public void asyncSecondDeleteByPattern(String pattern, String cacheName) {
        try {
            Thread.sleep(SECOND_DELETE_DELAY_MS);
            int count = deleteByPattern(pattern);
            log.debug("[缓存双删] 二删完成：{}（{} 条）", cacheName, count);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[缓存双删] 二删被中断：{}", cacheName);
        } catch (Exception e) {
            log.warn("[缓存双删] 二删失败：{}", cacheName, e);
        }
    }
}
