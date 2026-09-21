package com.reggie.module.ai.tool;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ToolFormats} 单元测试：任意数字形态统一两位小数，坏值回落 0.00。
 *
 * @author reggie
 * @since 2026-09-21
 */
class ToolFormatsTest {

    @Test
    void nullValueReturnsZero() {
        assertEquals("0.00", ToolFormats.money(null));
    }

    @Test
    void bigDecimalScaledHalfUp() {
        assertEquals("12.35", ToolFormats.money(new BigDecimal("12.345")));
    }

    @Test
    void numberTypesConverted() {
        assertEquals("8.00", ToolFormats.money(Integer.valueOf(8)));
        assertEquals("8.80", ToolFormats.money(Double.valueOf(8.8)));
    }

    @Test
    void numericStringConverted() {
        assertEquals("100.20", ToolFormats.money("100.2"));
    }

    @Test
    void malformedStringReturnsZero() {
        assertEquals("0.00", ToolFormats.money("abc"));
    }
}
