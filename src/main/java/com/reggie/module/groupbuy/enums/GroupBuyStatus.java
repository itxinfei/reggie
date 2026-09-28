package com.reggie.module.groupbuy.enums;

import lombok.Getter;

/**
 * <p>
 * 拼团相关状态枚举（活动状态 + 参与记录状态共用取值空间，按字段语义分别使用）。
 * 用于消除 {@code GroupBuyServiceImpl} 中的字符串魔法值。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-09-27
 */
@Getter
public enum GroupBuyStatus {

    /** 活动开放中 */
    OPEN("OPEN"),
    /** 活动成团（已达人数阈值） */
    CLOSED("CLOSED"),
    /** 活动未成团结束（触发退款） */
    ENDED("ENDED"),
    /** 参与记录：已参团（未支付） */
    JOINED("JOINED"),
    /** 参与记录：已支付 */
    PAID("PAID");

    private final String value;

    GroupBuyStatus(String value) {
        this.value = value;
    }
}
