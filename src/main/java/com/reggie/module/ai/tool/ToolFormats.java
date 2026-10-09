package com.reggie.module.ai.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 工具摘要格式化（金额/比率统一两位小数，避免给前端状态条与模型喂入奇怪格式）。
 *
 * @author reggie
 * @since 2026-09-20
 */
public final class ToolFormats {

    private ToolFormats() {
    }

    /** 任意数字形态（BigDecimal/Number/String）→ 两位小数字符串；null → 0.00 */
    public static String money(Object value) {
        if (value == null) {
            return "0.00";
        }
        try {
            BigDecimal bd = (value instanceof BigDecimal)
                    ? (BigDecimal) value : new BigDecimal(value.toString());
            return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }
}
