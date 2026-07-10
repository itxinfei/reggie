package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RechargeRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 会员ID */
    private Long memberId;
    /** 充值金额 */
    private BigDecimal amount;
    /** 赠送金额 */
    private BigDecimal giftAmount;
    /** 支付方式（wechat微信 alipay支付宝） */
    private String paymentMethod;
    /** 创建时间 */
    private LocalDateTime createdTime;
}
