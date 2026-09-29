package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手提现申请。
 * <p>状态：PENDING=待审批，APPROVED=已同意（已释放冻结，视为已打款），REJECTED=已拒绝（退回可提现）。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_withdrawal")
public class RiderWithdrawal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 骑手 ID */
    private Long riderId;

    /** 提现金额（元） */
    private BigDecimal amount;

    /** 状态 */
    private String status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核人 ID */
    private Long reviewerId;

    /** 审核人名称 */
    private String reviewerName;

    /** 拒绝原因 / 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
