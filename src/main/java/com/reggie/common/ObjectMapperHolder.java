package com.reggie.common;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ObjectMapper 统一持有工具类
 * <p>
 * 解决项目中多处 new ObjectMapper() 导致的资源浪费和配置不一致问题。
 * 提供两种获取方式：
 * <ul>
 *   <li>{@link #getDefault()} - 获取 JacksonObjectMapper 实例（含日期/Long序列化配置），适用于通用场景</li>
 *   <li>{@link #getRedisMapper()} - 获取 Redis 专用 ObjectMapper（含 DefaultTyping），仅在 Spring 容器初始化后可用</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-08-12
 */
public final class ObjectMapperHolder {

    /**
     * 通用 ObjectMapper 单例（JacksonObjectMapper，含日期/Long 序列化配置）
     * 线程安全：ObjectMapper 本身是线程安全的
     */
    private static final ObjectMapper DEFAULT_MAPPER = new JacksonObjectMapper();

    private ObjectMapperHolder() {
        throw new AssertionError();
    }

    /**
     * 获取通用 ObjectMapper 实例
     * <p>
     * 配置：FAIL_ON_UNKNOWN_PROPERTIES=false、Long→String、LocalDateTime 格式化
     * </p>
     *
     * @return JacksonObjectMapper 单例
     */
    public static ObjectMapper getDefault() {
        return DEFAULT_MAPPER;
    }

    /**
     * 获取 Redis 专用 ObjectMapper 实例（含 DefaultTyping）
     * <p>
     * 需要 Spring 容器已初始化，通过 ApplicationContextProvider 获取。
     * 适用于需要类型信息保留的 Redis 序列化场景。
     * </p>
     *
     * @return Redis ObjectMapper Bean，容器未就绪时回退到默认实例
     */
    public static ObjectMapper getRedisMapper() {
        try {
            ObjectMapper redisMapper = ApplicationContextProvider.getBean("redisObjectMapper", ObjectMapper.class);
            return redisMapper != null ? redisMapper : DEFAULT_MAPPER;
        } catch (Exception e) {
            // 容器未就绪或 Bean 不存在时回退
            return DEFAULT_MAPPER;
        }
    }
}
