package com.reggie.common;

public class BaseContext {
    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_THREAD_LOCAL = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        THREAD_LOCAL.set(id);
    }

    public static Long getCurrentId(){
        return THREAD_LOCAL.get();
    }

    public static void setCurrentTenantId(Long tenantId) {
        TENANT_THREAD_LOCAL.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return TENANT_THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
        TENANT_THREAD_LOCAL.remove();
    }
}
