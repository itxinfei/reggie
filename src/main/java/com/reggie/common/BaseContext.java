package com.reggie.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 基础上下文工具类
 * 使用ThreadLocal存储当前用户ID和租户ID
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
public class BaseContext {
    /**
     * 当前用户ID存储
     */
    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 当前租户ID存储
     */
    private static final ThreadLocal<Long> TENANT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 当前请求traceId存储（由 TraceIdFilter 注入，供业务层查询）
     */
    private static final ThreadLocal<String> TRACE_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     *
     * @param id 用户ID
     */
    public static void setCurrentId(Long id){
        THREAD_LOCAL.set(id);
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentId(){
        return THREAD_LOCAL.get();
    }

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setCurrentTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("租户ID为null，请检查登录逻辑");
        }
        TENANT_THREAD_LOCAL.set(tenantId);
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getCurrentTenantId() {
        return TENANT_THREAD_LOCAL.get();
    }

    /**
     * 设置当前请求traceId
     *
     * @param traceId 请求追踪ID
     */
    public static void setCurrentTraceId(String traceId) {
        TRACE_ID_THREAD_LOCAL.set(traceId);
    }

    /**
     * 获取当前请求traceId
     *
     * @return traceId
     */
    public static String getCurrentTraceId() {
        return TRACE_ID_THREAD_LOCAL.get();
    }

    /**
     * 清除当前请求traceId
     */
    public static void removeTraceId() {
        TRACE_ID_THREAD_LOCAL.remove();
    }

    /**
     * 清除ThreadLocal中的数据，防止内存泄漏
     */
    public static void remove() {
        THREAD_LOCAL.remove();
        TENANT_THREAD_LOCAL.remove();
        TRACE_ID_THREAD_LOCAL.remove();
    }
}
