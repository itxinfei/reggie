package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天响应DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponse {

    /** 回复内容 */
    private String content;

    /** 推荐菜品列表（点餐场景专用） */
    private List<AIRecommendedDish> dishes;

    /** 使用的模型 */
    private String model;

    /** Token使用量 */
    private Integer tokensUsed;
}
