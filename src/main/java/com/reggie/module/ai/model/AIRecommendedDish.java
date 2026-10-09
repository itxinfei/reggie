package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI推荐菜品
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendedDish {

    /** 菜品ID */
    private Long dishId;

    /** 菜品名称 */
    private String name;

    /** 菜品价格 */
    private BigDecimal price;

    /** 菜品图片 */
    private String image;

    /** 分类名称 */
    private String categoryName;

    /** 推荐理由 */
    private String reason;

    /** AI推荐权重 */
    private Double score;
}
