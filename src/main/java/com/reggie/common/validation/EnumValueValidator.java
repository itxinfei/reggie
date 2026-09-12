package com.reggie.common.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 枚举值校验器
 *
 * @author reggie
 * @since 2026-07-15
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Integer> {

    private int[] allowedValues;

    /**
     * 初始化 ialize。
     * @param constraintAnnotation 参数 constraintAnnotation
     */
    @Override
    public void initialize(EnumValue constraintAnnotation) {
        allowedValues = constraintAnnotation.values();
    }

    /**
     * 判断 valid。
     * @param value 参数 value
     * @param context 参数 context
     * @return 返回结果
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 由 @NotNull 处理
        }
        for (int allowed : allowedValues) {
            if (value == allowed) {
                return true;
            }
        }
        return false;
    }
}
