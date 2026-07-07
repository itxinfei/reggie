package com.reggie.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitAspect 集成测试
 *
 * @author itxinfei
 */
@SuppressWarnings("unchecked")
@SpringBootTest
@ActiveProfiles("test")
class RateLimitAspectTest {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRateLimitDisabledWhenRedisNull() {
        // Redis 为 null 时，限流应该自动禁用并降级
        RateLimitAspect aspect = new RateLimitAspect(null);
        assertFalse(aspect.isEnabled(), "Redis 为 null 时应该禁用限流");
    }

    @Test
    void testRateLimitEnabledWhenRedisAvailable() {
        // Redis 可用时，限流应该启用
        RateLimitAspect aspect = new RateLimitAspect(redisTemplate);
        assertTrue(aspect.isEnabled(), "Redis 可用时应该启用限流");
    }

    @Test
    void testRateLimitWithRedis() throws Exception {
        // 模拟 Redis 限流
        when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        when(redisTemplate.opsForValue().increment(anyString())).thenReturn(1L);

        RateLimitAspect aspect = new RateLimitAspect(redisTemplate);
        assertTrue(aspect.isEnabled());
    }
}
