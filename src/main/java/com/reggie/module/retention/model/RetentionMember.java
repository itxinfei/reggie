package com.reggie.module.retention.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 会员留存实体（Mock 数据，不映射数据库表）
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class RetentionMember {

    /** 主键ID */
    private Long id;

    /** 会员姓名 */
    private String memberName;

    /** 手机号 */
    private String phone;

    /** 等级: GOLD / SILVER / NORMAL */
    private String level;

    /** 积分 */
    private Integer points;

    /** 最后下单日期 */
    private LocalDate lastOrderDate;

    /** 总下单次数 */
    private Integer totalOrders;

    /** 总消费金额 */
    private BigDecimal totalSpent;

    /** 状态: ACTIVE / DORMANT / CHURNED */
    private String status;

    /** 标签 */
    private String tag;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
