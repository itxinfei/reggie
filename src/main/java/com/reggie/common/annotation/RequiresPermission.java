package com.reggie.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 用于Controller方法上，指定需要的权限标识
 * 示例：@RequiresPermission("dish:edit")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 需要的权限标识（支持多个，用逗号分隔）
     */
    String value();

    /**
     * 是否必须拥有所有权限（AND），默认false（OR）
     */
    boolean requireAll() default false;
}
