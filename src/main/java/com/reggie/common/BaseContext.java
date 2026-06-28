package com.reggie.common;

public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> tenantThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }

    public static void setCurrentTenantId(Long tenantId) {
        tenantThreadLocal.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return tenantThreadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
        tenantThreadLocal.remove();
    }
}
