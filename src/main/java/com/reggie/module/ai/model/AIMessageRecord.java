package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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

    /** 附件列表JSON（P2 视觉多模态：[{attachmentId,type,mime,url,width,height}]） */
    private String attachments;

    /** 消息类型：text/action/feedback */
    private String messageType;

    /** 消息状态：completed/stopped/failed */
    private String status;

    /** 前端消息幂等键（同一会话内重复提交只落一条用户消息） */
    private String clientMsgId;

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

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
