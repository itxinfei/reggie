package com.reggie.common.annotation;

import java.lang.annotation.*;

/**
 * 超级管理员鉴权注解
 * <p>
 * 标注在 Controller 类或方法上，限定仅超级管理员（roleKey=SUPER_ADMIN）可访问，
 * 用于系统管理类等高危接口，防止普通员工越权操作。
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresAdmin {
}
