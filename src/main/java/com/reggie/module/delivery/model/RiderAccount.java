package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手账户（与会员余额完全解耦）。
 * <p>每个骑手在每个租户下一条记录，记录可提现余额、冻结中金额、累计收入与累计已提现。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_account")
public class RiderAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 骑手 ID */
    private Long riderId;

    /** 可提现余额（元） */
    private BigDecimal withdrawableBalance;

    /** 冻结中金额（提现申请待审/已批未转账，元） */
    private BigDecimal frozenBalance;

    /** 累计收入（元，含已提现） */
    private BigDecimal totalIncome;

    /** 累计已提现（元） */
    private BigDecimal totalWithdrawn;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
