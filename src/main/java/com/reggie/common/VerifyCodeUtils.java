package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 * 支持 Redis（优先，集群部署）和 Session（降级，单机部署）
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
     * 保存验证码
     *
     * @param key         唯一标识（如手机号）
     * @param verifyCode  验证码
     * @param session     HTTP Session（降级使用）
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
                log.warn("保存验证码到 Redis 失败，降级到 Session: {}", e.getMessage());
                // Redis 保存失败，降级到 Session
            }
        }

        // Redis 不可用或保存失败，使用 Session（单机部署）
        session.setAttribute(key, verifyCode);
        log.debug("验证码已保存到 Session - key: {}", key);
    }

    /**
     * 校验验证码
     *
     * @param key         唯一标识（如手机号）
     * @param verifyCode  用户提交的验证码
     * @param session     HTTP Session（降级使用）
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
                log.warn("从 Redis 获取验证码失败，降级到 Session: {}", e.getMessage());
                // Redis 获取失败，降级到 Session
            }
        }

        // Redis 不可用或未找到，降级到 Session
        Object codeInSession = session.getAttribute(key);
        if (codeInSession != null && Objects.equals(codeInSession.toString(), verifyCode)) {
            // 验证通过，删除验证码（防止重复使用）
            session.removeAttribute(key);
            log.debug("验证码校验通过（Session）- key: {}", key);
            return true;
        }

        log.debug("验证码校验失败 - key: {}, verifyCode: {}", key, verifyCode);
        return false;
    }

    /**
     * 检查验证码是否存在（未验证）
     *
     * @param key     唯一标识（如手机号）
     * @param session HTTP Session（降级使用）
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
                log.warn("检查 Redis 验证码失败，降级到 Session: {}", e.getMessage());
            }
        }

        // 降级到 Session
        return session.getAttribute(key) != null;
    }

    /**
     * 清除验证码
     *
     * @param key     唯一标识（如手机号）
     * @param session HTTP Session（降级使用）
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

        // 清除 Session
        session.removeAttribute(key);
    }
}
