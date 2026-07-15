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

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        allowedValues = constraintAnnotation.values();
    }

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
