package com.reggie.module.delivery.model;

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
public class DeliveryOrder implements Serializable {
    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 租户ID */
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
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
    /** 创建人ID */
    private Long createdUser;
    /** 更新人ID */
    private Long updatedUser;
}
