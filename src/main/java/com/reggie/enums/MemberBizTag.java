package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 会员业务标签枚举，用于会员画像和精准营销
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Getter
public enum MemberBizTag {

    /** 高活跃 */
    HIGHLY_ACTIVE("HIGHLY_ACTIVE", "高活跃"),

    /** 高价值 */
    HIGH_VALUE("HIGH_VALUE", "高价值"),

    /** 新用户 */
    NEW_USER("NEW_USER", "新用户"),

    /** 流失预警 */
    LAPSED("LAPSED", "流失预警"),

    /** 促销敏感 */
    PROMOTION_SENSITIVE("PROMOTION_SENSITIVE", "促销敏感"),

    /** 美食家 */
    FOODIE("FOODIE", "美食家");

    private final String value;
    private final String desc;

    MemberBizTag(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
