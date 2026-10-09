package com.reggie.module.platform.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品平台映射表
 * <p>
 * 记录本系统菜品与平台菜品的映射关系，支持上架/下架/改价同步。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@TableName("dish_platform_mapping")
public class DishPlatformMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 本系统菜品ID */
    private Long dishId;

    /** 平台类型：MEITUAN / ELEME / DOUYIN / SELF / OTHER */
    private String platformType;

    /** 平台侧门店 ID */
    private String platformShopId;

    /** 平台菜品 ID */
    private String platformDishId;

    /** 平台 SKU ID */
    private String platformSkuId;

    /** 平台价格 */
    private BigDecimal price;

    /** 状态：0=下架，1=上架 */
    private Integer status;

    /** 租户 ID */
    private Long tenantId;

    /** 逻辑删除：0=未删，1=已删 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
