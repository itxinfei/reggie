package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话会话
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("ai_conversation")
@Schema(description = "AI对话会话")
public class AIConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", example = "1")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "会话UUID（前端生成）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String conversationId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "会话标题（首条消息摘要）", example = "帮我推荐菜品")
    private String title;

    @Schema(description = "AI场景：order_assistant=订单助手，dish_desc=菜品描述，business_analysis=经营分析，marketing=营销建议", example = "order_assistant")
    private String scene;

    @Schema(description = "消息数量", example = "5")
    private Integer messageCount;

    @Schema(description = "是否删除：0=否，1=是", example = "0")
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @Schema(description = "创建时间", example = "2026-07-10 12:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-07-10 12:05:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建人ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID", example = "1")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
