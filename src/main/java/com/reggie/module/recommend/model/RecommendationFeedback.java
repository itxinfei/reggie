package com.reggie.module.recommend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐反馈
 * 记录用户对推荐结果的反馈，用于优化推荐算法
 */
@Data
@TableName("recommendation_feedback")
public class RecommendationFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 反馈类型 - 点击 */
    public static final int FEEDBACK_CLICK = 1;
    /** 反馈类型 - 收藏 */
    public static final int FEEDBACK_FAVORITE = 2;
    /** 反馈类型 - 加购 */
    public static final int FEEDBACK_ADD_CART = 3;
    /** 反馈类型 - 下单 */
    public static final int FEEDBACK_ORDER = 4;
    /** 反馈类型 - 不感兴趣 */
    public static final int FEEDBACK_NOT_INTERESTED = 5;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 关联推荐缓存ID */
    private Long recommendCacheId;

    /** 菜品/套餐ID */
    private Long dishId;

    /** 反馈类型 */
    private Integer feedbackType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
