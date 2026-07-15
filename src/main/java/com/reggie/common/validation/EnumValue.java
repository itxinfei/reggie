package com.reggie.common.validation;

import com.reggie.enums.DishStatus;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 校验字段值必须是指定枚举集合中的一个值
 * <p>用于校验菜品状态等 Integer 类型的枚举字段</p>
 *
 * @author reggie
 * @since 2026-07-15
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

    String message() default "取值不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 允许的枚举值列表
     */
    int[] values() default {};
}
