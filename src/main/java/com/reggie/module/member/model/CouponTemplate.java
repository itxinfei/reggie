package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CouponTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 模板名称 */
    private String name;
    /** 类型（1满减券 2折扣券 3代金券） */
    private String type;
    /** 满减条件金额 */
    private BigDecimal conditionAmount;
    /** 优惠金额 */
    private BigDecimal discountAmount;
    /** 折扣率 */
    private BigDecimal discountRate;
    /** 发放总数 */
    private Integer totalCount;
    /** 剩余数量 */
    private Integer remainCount;
    /** 有效天数 */
    private Integer validDays;
    /** 状态（0禁用 1启用） */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
}
