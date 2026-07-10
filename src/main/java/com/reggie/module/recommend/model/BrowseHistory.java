package com.reggie.module.recommend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户浏览历史记录
 * 追踪用户在前端浏览菜品/套餐的行为
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("user_browse_history")
public class BrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 浏览对象类型 - 菜品 */
    public static final int TARGET_TYPE_DISH = 1;
    /** 浏览对象类型 - 套餐 */
    public static final int TARGET_TYPE_SETMEAL = 2;

    /** 行为类型 - 浏览 */
    public static final int ACTION_VIEW = 1;
    /** 行为类型 - 收藏 */
    public static final int ACTION_FAVORITE = 2;
    /** 行为类型 - 加购 */
    public static final int ACTION_ADD_CART = 3;
    /** 行为类型 - 分享 */
    public static final int ACTION_SHARE = 4;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 浏览对象类型 1:菜品 2:套餐 */
    private Integer targetType;

    /** 浏览对象ID */
    private Long targetId;

    /** 浏览对象名称 */
    private String targetName;

    /** 浏览停留时长(秒) */
    private Integer durationSeconds;

    /** 行为类型 1:浏览 2:收藏 3:加购 4:分享 */
    private Integer actionType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
