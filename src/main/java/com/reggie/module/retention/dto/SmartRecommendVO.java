package com.reggie.module.retention.dto;

import lombok.Data;

/**
 * 智能推荐 VO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
public class SmartRecommendVO {

    /** 会员ID */
    private Long memberId;

    /** 会员姓名 */
    private String memberIdCard;

    /** 推荐类型 */
    private String recommendType;

    /** 推荐内容 */
    private String recommendContent;

    /** 推荐原因 */
    private String reason;

    /** 优先级: HIGH / MEDIUM / LOW */
    private String priority;
}