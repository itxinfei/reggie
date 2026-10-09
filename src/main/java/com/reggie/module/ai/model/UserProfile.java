package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI用户画像
 * 基于历史对话、点单记录、反馈数据构建用户长期记忆
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("ai_user_profile")
public class UserProfile {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    // ========== 口味偏好 ==========

    /** 偏好的口味标签（JSON数组），如 ["辣", "清淡", "甜"] */
    private String tasteTags;

    /** 偏好的菜品分类（JSON数组），如 ["热菜", "小吃", "饮品"] */
    private String categoryTags;

    /** 不喜欢的口味标签（JSON数组），如 ["香菜", "蒜"] */
    private String dislikedTags;

    /** 忌口/过敏信息 */
    private String allergies;

    // ========== 消费偏好 ==========

    /** 价格偏好标签：budget/economy/standard/premium */
    private String pricePreference;

    /** 客单价均值（分） */
    private Integer avgOrderAmount;

    /** 常用就餐人数 */
    private Integer usualDiners;

    // ========== 行为特征 ==========

    /** 用户标签（JSON数组），如 ["高频", "夜宵", "企业用户"] */
    private String userTags;

    /** 常点菜品ID列表（JSON数组），Top 10 */
    private String frequentDishIds;

    /** 偏好的配送方式：delivery/dine_in/pickup */
    private String preferredDiningType;

    /** 偏好的送达时段：morning/lunch/dinner/late_night */
    private String preferredTimeSlot;

    /** 是否在意配送费 */
    private Boolean deliveryFeeSensitive;

    // ========== 画像质量 ==========

    /** 画像置信度 0.00~1.00（数据越多越高） */
    private BigDecimal confidence;

    /** 最后更新画像的时间 */
    private LocalDateTime lastAnalyzedTime;

    /** 总对话次数 */
    private Integer totalConversations;

    /** 总反馈次数 */
    private Integer totalFeedbacks;

    // ========== 审计字段 ==========

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
