package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送订单实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("delivery_order")
public class DeliveryOrder implements Serializable {
    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
    /** 平台订单号 */
    private String platformOrderId;
    /** 配送平台 */
    private String platform;
    /** 菜品摘要 */
    private String dishSummary;
    /** 订单金额 */
    private BigDecimal amount;
    /** 用户姓名 */
    private String userName;
    /** 联系电话 */
    private String phone;
    /** 配送地址 */
    private String address;
    /** 订单状态 */
    private String status;
    /** 下单时间 */
    private LocalDateTime orderTime;
    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 创建人ID */
    @TableField("created_user")
    private Long createdUser;
    /** 更新人ID */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
