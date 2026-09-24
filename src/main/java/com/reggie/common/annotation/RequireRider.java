package com.reggie.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口仅允许骑手（配送员）会话访问，拒绝员工、C 端顾客会话。
 * <p>
 * 与 {@link RequireEmployee}（员工）对称：本注解只校验"当前会话是否为骑手会话
 * （request 属性 riderId 存在）"，用于骑手端 H5 的业务接口（我的任务、抢单、
 * 取餐/送达确认等）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRider {
}
