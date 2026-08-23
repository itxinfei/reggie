package com.reggie.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 租户隔离一致性标记注解（供后续域4 清理时标注需要移除冗余 {@code .eq(...getTenantId...)} 的方法）。
 * </p>
 *
 * <p>
 * 当前注解无功能副作用，仅作为标记使用。域4 代码结构优化阶段将统一清理被标注方法中的
 * 冗余租户过滤条件（因为 TenantLineInnerInterceptor 已在 MyBatis 层自动追加 {@code WHERE tenant_id}）。
 * </p>
 *
 * @author AI
 * @since 2026-08-22
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresTenantIsolation {

    /**
     * 目标实体类名（用于代码审查时的快速定位）
     */
    String[] entities() default {};
}