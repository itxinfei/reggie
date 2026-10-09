package com.reggie.module.recommend.model;

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
 * 用户偏好标签
 * 基于用户历史订单、浏览记录分析得出的口味/品类/价格偏好
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("user_preference_tag")
public class UserPreferenceTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签类型 - 口味偏好 */
    public static final int TAG_TYPE_TASTE = 1;
    /** 标签类型 - 品类偏好 */
    public static final int TAG_TYPE_CATEGORY = 2;
    /** 标签类型 - 价格偏好 */
    public static final int TAG_TYPE_PRICE = 3;
    /** 标签类型 - 时段偏好 */
    public static final int TAG_TYPE_TIME = 4;

    /** 数据来源 - 订单分析 */
    public static final String SOURCE_ORDER = "ORDER";
    /** 数据来源 - 浏览分析 */
    public static final String SOURCE_BROWSE = "BROWSE";
    /** 数据来源 - 手动标注 */
    public static final String SOURCE_MANUAL = "MANUAL";

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 标签类型 1:口味偏好 2:品类偏好 3:价格偏好 4:时段偏好 */
    private Integer tagType;

    /** 标签名称 */
    private String tagName;

    /** 偏好权重 0.00~1.00 */
    private BigDecimal tagValue;

    /** 数据来源 ORDER/BROWSE/MANUAL */
    private String source;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
