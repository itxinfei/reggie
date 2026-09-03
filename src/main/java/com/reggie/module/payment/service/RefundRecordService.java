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

    /**
     * 用户端发起售后申请（整单退款）。
     *
     * <p>校验：订单存在、归属当前用户、状态为已完成；同订单已有 PENDING 申请则拒绝重复申请。
     * 售后金额取订单实付金额（amount + deliveryFee），售后类型默认整单退款。</p>
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 创建的退款记录（含 refundNo，状态 PENDING）
     */
    RefundRecord applyUserRefund(Long orderId, String reason);

    /**
     * 用户端查询某订单的售后申请记录。
     *
     * @param orderId 订单ID
     * @return 退款记录列表（按创建时间倒序）
     */
    List<RefundRecord> listUserRefundByOrderId(Long orderId);

    /**
     * 持久化"渠道已退款但本地落库失败"的对账待办痕迹。
     * <p>
     * 使用独立事务（REQUIRES_NEW），确保外层事务回滚后该痕迹仍存活，供对账定时任务扫描告警人工核对。
     * 痕迹以 {@code [对账待办]} 前缀的 reason 标识，状态为 PENDING。
     * </p>
     * <p>
     * <b>禁止</b>据此自动重试渠道退款——渠道侧可能已退款成功，重试会导致重复退款。
     * 仅作可观测痕迹，由人工核对渠道后台后决定是否标记 SUCCESS。
     * </p>
     *
     * @param paymentOrderId 支付单ID
     * @param amount         退款金额
     * @param reason         原始退款原因
     */
    void recordReconcileTrace(Long paymentOrderId, BigDecimal amount, String reason);
}
