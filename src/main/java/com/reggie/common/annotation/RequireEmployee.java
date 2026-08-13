package com.reggie.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口仅允许员工（后台账号）访问，拒绝 C 端顾客会话。
 * <p>
 * 与 {@link RequiresAdmin}（仅超级管理员）/ {@link RequiresPermission}（需具体权限）互补：
 * 本注解只校验"当前会话是否为员工会话（request 属性 employeeId 存在）"，
 * 不限制具体角色或权限，用于普通后台业务管理接口。
 * </p>
 * <p>
 * 根因说明：{@code LoginCheckFilter} 仅校验"是否登录"，不区分 employee（员工）与 user（C 端顾客）。
 * 仅依赖登录态的后台接口，顾客登录后亦可越权访问。本注解用于收紧这类接口，
 * 确保只有员工会话可以进入（门店店长 STORE_MANAGER 属员工会话，不受影响）。
 * </p>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireEmployee {
}
