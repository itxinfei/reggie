package com.reggie.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitAspect 单元测试
 * <p>
 * 纯 Mockito 单测：不加载 Spring 容器，直接构造 aspect 验证限流启用/禁用逻辑。
 *
 * @author itxinfei
 */
@SuppressWarnings("unchecked")
class RateLimitAspectTest {

    @Test
    void testRateLimitDisabledWhenRedisNull() {
        // Redis 为 null 时，限流应该自动禁用并降级
        RateLimitAspect aspect = new RateLimitAspect(null);
        assertFalse(aspect.isEnabled(), "Redis 为 null 时应该禁用限流");
    }

    @Test
    void testRateLimitEnabledWhenRedisAvailable() {
        // Redis 可用时，限流应该启用
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RateLimitAspect aspect = new RateLimitAspect(redisTemplate);
        assertTrue(aspect.isEnabled(), "Redis 可用时应该启用限流");
    }

    @Test
    void testRateLimitWithRedis() throws Exception {
        // 模拟 Redis 限流
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForValue().increment(anyString())).thenReturn(1L);

        RateLimitAspect aspect = new RateLimitAspect(redisTemplate);
        assertTrue(aspect.isEnabled());
    }
}
