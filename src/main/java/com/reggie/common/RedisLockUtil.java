package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 分布式锁通用工具（SET NX EX 加锁 + Lua 脚本原子释放）。
 *
 * <p>收敛自 7 个定时任务（OrderTimeoutTask / CouponExpirationTask / PointsExpireTask /
 * GroupBuyExpireTask / RiderDispatchTimeoutTask / StockRefundCompensationTask /
 * UnacceptedOrderScanTask）及 platform 三个任务中逐字复制的 tryLock/unlock + Lua 副本，
 * 并统一降级语义为 <b>fail-closed</b>。</p>
 *
 * <p>使用约定：</p>
 * <ul>
 *   <li>锁值必须使用唯一标识（{@link #newLockValue()} 生成的 UUID），释放时通过 Lua 脚本
 *       比对锁值，只有持有者本人才能删除，防止误删他人的锁，同时消除 get+delete 的 TOCTOU 竞态；</li>
 *   <li>若手工调用 {@link #tryLock} 而非 {@link #executeWithLock}，业务体必须放在
 *       {@code try { ... } finally { unlockIfOwned(key, value); }} 中，确保异常路径也释放锁；</li>
 *   <li><b>fail-closed 策略（全环境一致，含 dev）</b>：Redis 模板不可用、加锁失败或加锁抛异常，
 *       一律按"未拿到锁"处理——定时任务侧应跳过本轮（另一实例正在执行或 Redis 故障），
 *       绝不无锁裸跑，避免多实例重复扣款/取消订单/回退库存等资损操作。
 *       拿不到锁记 log.info，Redis 异常在 {@link #tryLock} 内部记 log.error。</li>
 * </ul>
 *
 * <p>模板类型与各任务现用 {@code RedisTemplate<String, Object>}（RedisConfig 中定义）保持一致，
 * 确保存量锁 key/value 序列化行为等价。</p>
 *
 * @author reggie
 * @since 2026-10-02
 */
@Slf4j
@Component
public class RedisLockUtil {

    /**
     * 释放锁的 Lua 脚本：比对锁值后才删除，防止误删他人的锁；
     * 原子执行消除 get+delete 之间的竞态窗口。
     */
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);

    /** Redis 模板（可选注入；不可用时按 fail-closed 处理，锁一律获取失败） */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成新的锁值（UUID），用于标识锁持有者，释放时校验。
     */
    public static String newLockValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * 尝试获取分布式锁：SET NX EX 原子操作，不等待、不自旋。
     *
     * <p>fail-closed：Redis 模板不可用或加锁过程抛异常时返回 false（并记 log.error），
     * 调用方应视为"另一实例正在执行或 Redis 故障"而跳过本轮。</p>
     *
     * @param key           锁 Key
     * @param value         锁值（建议使用 {@link #newLockValue()}，释放时须原样传入比对）
     * @param expireSeconds 锁过期时间（秒），应大于任务单次最大执行时间
     * @return 是否成功持有锁
     */
    public boolean tryLock(String key, String value, long expireSeconds) {
        if (redisTemplate == null) {
            // fail-closed：Redis 未配置/不可用时视为未拿到锁，避免多实例（或无保护下）重复执行
            log.error("[RedisLock] RedisTemplate不可用，按获取分布式锁失败处理（fail-closed）: {}", key);
            return false;
        }
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            // fail-closed：加锁异常同样按未拿到锁处理，调用方跳过本轮
            log.error("[RedisLock] 获取分布式锁异常，按失败处理（fail-closed）: {}", key, e);
            return false;
        }
    }

    /**
     * 释放分布式锁：Lua 脚本原子校验锁值后才删除（value 匹配才删，语义与原任务私有 unlock 一致）。
     *
     * @param key   锁 Key
     * @param value 加锁时使用的锁值（UUID）
     * @return 是否真正删除了锁（值不匹配或 Redis 异常返回 false）
     */
    public boolean unlockIfOwned(String key, String value) {
        if (redisTemplate == null || value == null) {
            return false;
        }
        try {
            Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
            return result != null && result > 0;
        } catch (Exception e) {
            // 宽异常兜底：释放失败仅记录日志，锁会随 TTL 自动过期，不影响业务结果
            log.error("[RedisLock] 释放分布式锁失败: {}", key, e);
            return false;
        }
    }

    /**
     * 在分布式锁保护下执行有返回值的业务体（推荐用法，保证 finally 释放）。
     *
     * <p>fail-closed：未拿到锁（另一实例在执行或 Redis 异常）时不执行业务体，
     * 记 log.info 后返回 null。</p>
     *
     * @param key           锁 Key
     * @param expireSeconds 锁过期时间（秒）
     * @param action        业务体
     * @param <T>           返回值类型
     * @return 业务体返回值；未拿到锁时为 null
     */
    public <T> T executeWithLock(String key, long expireSeconds, Supplier<T> action) {
        String lockValue = newLockValue();
        if (!tryLock(key, lockValue, expireSeconds)) {
            log.info("[RedisLock] 未获取到锁（另一实例正在执行或 Redis 不可用），跳过本轮: {}", key);
            return null;
        }
        try {
            return action.get();
        } finally {
            unlockIfOwned(key, lockValue);
        }
    }

    /**
     * 在分布式锁保护下执行无返回值的业务体（定时任务常用，保证 finally 释放）。
     *
     * <p>fail-closed：未拿到锁（另一实例在执行或 Redis 异常）时不执行业务体，仅记 log.info。</p>
     *
     * @param key           锁 Key
     * @param expireSeconds 锁过期时间（秒）
     * @param action        业务体
     * @return 是否真正执行了业务体（即成功持有锁）
     */
    public boolean executeWithLock(String key, long expireSeconds, Runnable action) {
        String lockValue = newLockValue();
        if (!tryLock(key, lockValue, expireSeconds)) {
            log.info("[RedisLock] 未获取到锁（另一实例正在执行或 Redis 不可用），跳过本轮: {}", key);
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            unlockIfOwned(key, lockValue);
        }
    }
}
