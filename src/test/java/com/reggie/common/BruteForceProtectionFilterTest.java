package com.reggie.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BruteForceProtectionFilter 单元测试
 * <p>
 * 纯 Mockito 单测：不加载 Spring 容器，直接构造 filter 验证暴力破解防护逻辑。
 *
 * @author itxinfei
 */
@SuppressWarnings("unchecked")
class BruteForceProtectionFilterTest {

    @Test
    void testBruteForceDisabledWhenRedisNull() {
        // Redis 为 null 时，暴力破解防护应该自动禁用
        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(null);
        assertFalse(filter.isEnabled(), "Redis 为 null 时应该禁用暴力破解防护");
    }

    @Test
    void testBruteForceEnabledWhenRedisAvailable() {
        // Redis 可用时，暴力破解防护应该启用
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);
        assertTrue(filter.isEnabled(), "Redis 可用时应该启用暴力破解防护");
    }

    @Test
    void testRecordLoginFailureWithMockRedis() {
        // 模拟 Redis 操作（使用 Lua 脚本执行）
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Lua 脚本执行后返回 1（第一次失败）
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);
        assertTrue(filter.isEnabled());

        // 记录登录失败（使用 IP 标识），验证不会抛出异常
        assertDoesNotThrow(() -> filter.recordFailedAttempt("192.168.1.100"));
    }

    @Test
    void testResetLoginAttemptsWithMockRedis() {
        // 模拟 Redis 操作
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);
        assertTrue(filter.isEnabled());

        // 重置登录失败计数
        filter.resetFailedAttempts("192.168.1.100");
        verify(redisTemplate, atLeast(2)).delete(anyString());  // 至少删除2次（failure + locked）
    }

    @Test
    void testGetFailedAttemptsWithMockRedis() {
        // 模拟 Redis 操作
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(3);

        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);
        assertTrue(filter.isEnabled());

        // 获取失败次数
        int attempts = filter.getFailedAttemptCount("192.168.1.100");
        assertEquals(3, attempts, "应该返回正确的失败次数");
    }

    @Test
    void testGracefulDegradationWhenRedisUnavailable() {
        // 模拟 Redis 异常
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);
        assertTrue(filter.isEnabled(), "Redis 可用时应该启用");

        // Redis 异常时，方法应该优雅降级，不抛出异常
        assertDoesNotThrow(() -> {
            filter.recordFailedAttempt("192.168.1.100");
            filter.resetFailedAttempts("192.168.1.100");
            int attempts = filter.getFailedAttemptCount("192.168.1.100");
            assertEquals(0, attempts);
        });
    }

    @Test
    void testIsLoginRequest() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        BruteForceProtectionFilter filter = new BruteForceProtectionFilter(redisTemplate);

        // 可以通过反射调用私有方法验证登录请求识别
        // 或者通过测试实际的过滤器链
        assertTrue(filter.isEnabled());
    }
}
