package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 * 支持 Redis（优先，集群部署）和 本地内存（降级，单机部署）
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class VerifyCodeUtils {

    /**
     * 验证码存储 Key 前缀
     */
    private static final String VERIFY_CODE_KEY_PREFIX = "verify:code:";

    /**
     * 验证码过期时间（秒）
     */
    private static final int VERIFY_CODE_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * Redis操作模板，可选依赖
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 本地内存降级缓存（含时间戳，模拟 TTL）
     * Key: 验证码key, Value: [验证码, 创建时间戳]
     */
    private final Map<String, CodeEntry> localCache = new ConcurrentHashMap<>();

    /**
     * 本地缓存条目
     */
    private static class CodeEntry {
        final String code;
        final long createdAt;

        CodeEntry(String code) {
            this.code = code;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > VERIFY_CODE_EXPIRE_SECONDS * 1000L;
        }
    }

    /**
     * 保存验证码
     *
     * @param key         唯一标识（如手机号）
     * @param verifyCode  验证码
     * @param session     HTTP Session（保留兼容）
     */
    public void saveVerifyCode(String key, String verifyCode, HttpSession session) {
        if (key == null || verifyCode == null) {
            return;
        }

        // 优先使用 Redis（支持集群部署）
        if (redisTemplate != null) {
            try {
                String redisKey = VERIFY_CODE_KEY_PREFIX + key;
                redisTemplate.opsForValue().set(redisKey, verifyCode, VERIFY_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.debug("验证码已保存到 Redis - key: {}, expire: {}s", key, VERIFY_CODE_EXPIRE_SECONDS);
                return;
            } catch (Exception e) {
                log.warn("保存验证码到 Redis 失败，降级到本地内存: key={}, error={}", key, e.getMessage());
                // Redis 保存失败，降级到本地内存
            }
        }

        // 降级到本地内存 + 告警（集群部署时此降级不可靠）
        localCache.put(key, new CodeEntry(verifyCode));
        if (redisTemplate == null) {
            log.warn("[验证码] Redis 不可用，使用本地内存缓存(集群环境验证码不可靠！): key={}", key);
        }
    }

    /**
     * 校验验证码
     *
     * @param key         唯一标识（如手机号）
     * @param verifyCode  用户提交的验证码
     * @param session     HTTP Session（保留兼容）
     * @return true=验证通过，false=验证失败
     */
    public boolean verifyCode(String key, String verifyCode, HttpSession session) {
        if (key == null || verifyCode == null) {
            return false;
        }

        // 优先从 Redis 获取
        if (redisTemplate != null) {
            try {
                String redisKey = VERIFY_CODE_KEY_PREFIX + key;
                Object storedCode = redisTemplate.opsForValue().get(redisKey);
                if (storedCode != null && Objects.equals(storedCode.toString(), verifyCode)) {
                    // 验证通过，删除验证码（防止重复使用）
                    redisTemplate.delete(redisKey);
                    log.debug("验证码校验通过（Redis）- key: {}", key);
                    return true;
                }
            } catch (Exception e) {
                log.warn("从 Redis 获取验证码失败，降级到本地内存: key={}, error={}", key, e.getMessage());
            }
        }

        // 降级到本地内存（带 TTL 检查）
        CodeEntry entry = localCache.get(key);
        if (entry != null) {
            // 检查是否过期
            if (entry.isExpired()) {
                localCache.remove(key);
                log.debug("验证码已过期（本地缓存）- key: {}", key);
                return false;
            }
            if (Objects.equals(entry.code, verifyCode)) {
                localCache.remove(key);
                log.debug("验证码校验通过（本地缓存）- key: {}", key);
                return true;
            }
        }

        log.debug("验证码校验失败 - key: {}", key);
        return false;
    }

    /**
     * 检查验证码是否存在（未验证）
     *
     * @param key     唯一标识（如手机号）
     * @param session HTTP Session（保留兼容）
     * @return true=存在，false=不存在
     */
    public boolean hasVerifyCode(String key, HttpSession session) {
        if (key == null) {
            return false;
        }

        // 优先检查 Redis
        if (redisTemplate != null) {
            try {
                String redisKey = VERIFY_CODE_KEY_PREFIX + key;
                return redisTemplate.hasKey(redisKey);
            } catch (Exception e) {
                log.warn("检查 Redis 验证码失败，降级到本地内存: key={}, error={}", key, e.getMessage());
            }
        }

        // 降级到本地内存
        CodeEntry entry = localCache.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                localCache.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 清除验证码
     *
     * @param key     唯一标识（如手机号）
     * @param session HTTP Session（保留兼容）
     */
    public void clearVerifyCode(String key, HttpSession session) {
        if (key == null) {
            return;
        }

        // 清除 Redis
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(VERIFY_CODE_KEY_PREFIX + key);
            } catch (Exception e) {
                log.warn("清除 Redis 验证码失败: {}", e.getMessage());
            }
        }

        // 清除本地缓存
        localCache.remove(key);
    }
}
