package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI聊天请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatRequest {

    /** 用户消息 */
    private String message;

    /** 对话场景：order_assistant / dish_desc / business_analysis / marketing */
    private String scene;

    /** 上下文数据（菜品数据、经营数据等） */
    private Map<String, Object> context;

    /** 会话ID */
    private String conversationId;

    /** 用户ID（用于个性化推荐和画像注入） */
    private Long userId;
}
