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
     * 清除ThreadLocal中的数据，防止内存泄漏
     */
    public static void remove() {
        THREAD_LOCAL.remove();
        TENANT_THREAD_LOCAL.remove();
    }
}
