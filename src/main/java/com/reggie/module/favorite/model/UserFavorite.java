package com.reggie.module.favorite.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏（通用关联表）。
 * <p>取消收藏走物理删除（与购物车一致），配合唯一索引支持反复收藏。本期仅落地菜品收藏。</p>
 */
@Data
@TableName("user_favorite")
@Schema(description = "用户收藏")
public class UserFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收藏对象类型：菜品 */
    public static final int TYPE_DISH = 1;
    /** 收藏对象类型：商家（预留，暂未开放） */
    public static final int TYPE_STORE = 2;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "收藏类型：1=菜品，2=商家")
    @TableField("target_type")
    private Integer targetType;

    @Schema(description = "收藏对象ID（菜品ID / 商家租户ID）")
    @TableField("target_id")
    private Long targetId;

    @Schema(description = "租户ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "收藏时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
