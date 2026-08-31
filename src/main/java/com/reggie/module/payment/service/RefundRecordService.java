package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.payment.model.RefundRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 退款记录服务接口
 * </p>
 * <p>管理退款申请及退款状态追踪</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface RefundRecordService extends IService<RefundRecord> {

    /**
     * 根据订单ID查询退款记录
     *
     * @param orderId 订单ID
     * @return 退款记录列表
     */
    List<RefundRecord> listByOrderId(Long orderId);

    /**
     * 创建退款记录
     *
     * @param paymentOrderId 支付订单ID
     * @param amount         退款金额
     * @param reason         退款原因
     * @return 退款记录
     */
    RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason);

    /**
     * 更新退款记录状态为成功（渠道退款成功后调用，修复原先记录永远停留在 PENDING 的问题）。
     *
     * @param refundNo 退款流水号
     */
    void markRefundSuccess(String refundNo);

    /**
     * 查询某支付单已成功退款的总金额（用于退款累计超额校验）。
     *
     * @param paymentOrderId 支付单ID
     * @return 已退款总额
     */
    BigDecimal sumRefundedAmount(Long paymentOrderId);

    /**
     * 退款分析（当前租户）：总数/成功/退款中/失败 + 成功退款总额 + 退款原因 TOP5
     *
     * @param tenantId 租户ID（为空取当前上下文）
     * @return 分析结果 Map
     */
    java.util.Map<String, Object> getRefundAnalysis(Long tenantId);
}
