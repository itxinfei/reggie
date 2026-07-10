package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI对话会话
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("ai_conversation")
public class AIConversation {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话ID (前端生成 UUID) */
    private String conversationId;

    /** 用户ID（关联登录用户） */
    private Long userId;

    /** 会话标题（首条消息摘要） */
    private String title;

    /** 场景：order_assistant/dish_desc/business_analysis/marketing */
    private String scene;

    /** 消息数量 */
    private Integer messageCount;

    /** 是否删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
