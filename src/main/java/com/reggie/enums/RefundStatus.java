package com.reggie.enums;

import java.util.Arrays;

/**
 * <p>
 * 退款状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public enum RefundStatus {

    /**
     * 退款中
     */
    PENDING("pending", "退款中"),

    /**
     * 退款成功
     */
    SUCCESS("SUCCESS", "退款成功"),

    /**
     * 退款失败
     */
    FAIL("fail", "退款失败");

    private final String code;
    private final String desc;

    RefundStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 反向获取枚举
     */
    public static RefundStatus fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
