package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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

    /** 归属身份类型：EMPLOYEE/CUSTOMER（由控制器从登录会话解析，防止员工与用户ID撞号） */
    private String actorType;

    /** 前端消息幂等键（同一会话重复提交只落一条用户消息；重生成时为空） */
    private String clientMsgId;

    /** 是否重新生成上一条回答（为 true 时 message 可为空，服务端重放最后一条用户消息） */
    private Boolean regenerate;

    /** 附件ID列表（P2 视觉多模态启用，P1 仅透传字段） */
    private List<String> attachments;
}
