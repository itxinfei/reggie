package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI消息记录
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("ai_message")
public class AIMessageRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话ID */
    private String conversationId;

    /** 用户ID */
    private Long userId;

    /** 角色：user / assistant */
    private String role;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息类型：text/action/feedback */
    private String messageType;

    /** 反馈类型：good / bad / null */
    private String feedback;

    /** 推荐菜品ID列表（JSON） */
    private String dishIds;

    /** Token使用量 */
    private Integer tokensUsed;

    /** 是否删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
