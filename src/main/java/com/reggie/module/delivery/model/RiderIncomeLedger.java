package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手收入流水（幂等入账用）。
 * <p>以 {@code order_id} 唯一，确保一笔配送订单在 {@code deliverRiderOrder} 中只入账一次，
 * 防止重试/并发导致重复结算。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_income_ledger")
public class RiderIncomeLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 骑手 ID */
    private Long riderId;

    /** 订单 ID（唯一，幂等键） */
    private Long orderId;

    /** 本单入账金额（配送费，元） */
    private BigDecimal amount;

    /** 状态：1=已入账 */
    private Integer status;

    private LocalDateTime createTime;
}
