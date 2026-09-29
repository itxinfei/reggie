package com.reggie.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口仅允许 C 端顾客（用户）会话访问，拒绝员工、骑手会话。
 * <p>
 * 与 {@link RequireEmployee}（员工）、{@link RequireRider}（骑手）对称：本注解只校验
 * "当前会话是否为顾客会话（session 属性 user 存在）"，用于顾客端 H5 的业务接口
 * （提交评价、查询我的评价等）。切面在 {@code LoginCheckFilter} 未生效的测试/MockMvc
 * 等场景下兜底写入 {@code BaseContext}，保证 Controller 能取到当前用户与其租户。
 * </p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireUser {
}
