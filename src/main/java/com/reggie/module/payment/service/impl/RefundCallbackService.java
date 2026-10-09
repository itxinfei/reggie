package com.reggie.module.payment.service.impl;

import com.reggie.common.BaseContext;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderStockRefundService;
import com.reggie.module.payment.channel.notify.RefundNotifyResult;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 退款异步回调处理服务（微信 APIv3 退款终态通知）。
 * <p>
 * 微信退款可能在<b>同步</b>返回 PROCESSING（已受理、待银行/渠道确认），此时三处退款发起入口
 * 仅登记 PROCESSING 记录、不做任何联动；最终结果由微信退款异步通知推送到本服务，定终态：
 * </p>
 * <ul>
 *   <li>SUCCESS 且<b>首次</b>（CAS PROCESSING→SUCCESS 成功）：按累计退款额判定全额，
 *       全额时支付单 SUCCESS→REFUND、订单→REFUNDED、回补库存、回退会员权益；部分退款不翻状态；
 *       售后（用户申请）行额外补齐售后单成功语义。重复回调幂等跳过。</li>
 *   <li>CLOSED/ABNORMAL：PROCESSING→FAIL，告警人工核对渠道后台。</li>
 * </ul>
 * <p>
 * 回调无登录态/无租户 ThreadLocal：先以 {@code @InterceptorIgnore} 的跨租户 Mapper 定位记录，
 * 再按记录 tenantId 设置 BaseContext（finally 还原），随后复用租户内 service 方法。
 * 各副作用失败仅日志，由补偿任务/对账人工兜底，避免渠道已退款而本地无限重试。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class RefundCallbackService {

    /** 渠道退款成功终态 */
    private static final String STATUS_SUCCESS = "SUCCESS";
    /** 渠道退款异常/关闭终态（资金未确定退或需人工介入） */
    private static final List<String> FAIL_STATUS = Arrays.asList("CLOSED", "ABNORMAL");

    /** 跨租户定位退款记录 */
    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    @Lazy
    private OrderService orderService;

    @Autowired
    private OrderStockRefundService orderStockRefundService;

    @Autowired
    private MemberRewardService memberRewardService;

    /**
     * 处理一次退款异步通知（调用方已完成渠道验签/解密）。
     *
     * @param notify 渠道解析后的退款通知
     * @return true=已按终态处理（或重复回调），可回渠道成功 ACK；false=暂无法处理（本地缺记录/状态未知），回失败 ACK 触发重试
     */
    public boolean handleRefundNotify(RefundNotifyResult notify) {
        if (notify == null) {
            return false;
        }
        String refundNo = notify.getOutRefundNo();
        if (refundNo == null || refundNo.trim().isEmpty()) {
            log.warn("[退款回调] 缺少商户退款单号 outRefundNo，无法处理: {}", notify.getErrorMsg());
            return false;
        }
        RefundRecord record = refundRecordMapper.selectByRefundNoIgnoreTenant(refundNo);
        if (record == null) {
            // 本地无记录：可能是落库滞后，回失败 ACK 让渠道重试；仍缺则由对账告警人工
            log.warn("[退款回调] 本地无对应退款记录，等待渠道重试/人工核对: refundNo={}", refundNo);
            return false;
        }
        Long prevTenant = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(record.getTenantId());
            String status = notify.getStatus();
            if (STATUS_SUCCESS.equals(status)) {
                return onSuccess(record);
            }
            if (status != null && FAIL_STATUS.contains(status)) {
                refundRecordService.markProcessingToFail(refundNo, "渠道终态: " + status);
                log.error("[退款回调] 退款被渠道置异常终态，需人工处理: refundNo={}, status={}", refundNo, status);
                return true;
            }
            // 未识别状态：保持 PROCESSING，等待后续确定终态通知
            log.warn("[退款回调] 未识别的退款状态，保持处理中: refundNo={}, status={}", refundNo, status);
            return false;
        } finally {
            if (prevTenant == null) {
                BaseContext.remove();
            } else {
                BaseContext.setCurrentTenantId(prevTenant);
            }
        }
    }

    /**
     * 终态成功：CAS 首次确认后执行一次性联动；重复回调幂等跳过。
     */
    private boolean onSuccess(RefundRecord record) {
        String refundNo = record.getRefundNo();
        boolean firstTime = refundRecordService.markProcessingToSuccess(refundNo);
        if (!firstTime) {
            log.info("[退款回调] 退款已为成功终态，重复回调幂等跳过: refundNo={}", refundNo);
            return true;
        }
        // 售后（用户申请）行：补齐售后单成功语义并将订单置已退款（方法内部幂等）
        if (record.getApplyUserId() != null) {
            refundRecordService.markUserRefundSuccess(refundNo);
        }
        applyFullRefundSideEffects(record);
        return true;
    }

    /**
     * 按累计成功退款额判定全额联动。
     * <p>
     * 累计已退 == 支付额（全额）：支付单 CAS SUCCESS→REFUND、订单（2/3/4）→REFUNDED、
     * 回补菜品+原料库存、回退积分/优惠券；部分退款不翻状态、不联动。
     * 库存/积分失败仅日志，由补偿任务与对账人工兜底。
     * </p>
     */
    private void applyFullRefundSideEffects(RefundRecord record) {
        Long paymentOrderId = record.getPaymentOrderId();
        PaymentOrder paymentOrder = paymentOrderService.getById(paymentOrderId);
        if (paymentOrder == null) {
            log.warn("[退款回调] 支付单不存在，跳过全额联动: paymentOrderId={}", paymentOrderId);
            return;
        }
        BigDecimal refunded = refundRecordService.sumRefundedAmount(paymentOrderId);
        if (paymentOrder.getAmount() == null
                || refunded.compareTo(paymentOrder.getAmount()) != 0) {
            log.info("[退款回调] 部分退款不翻状态: paymentOrderId={}, 已退={}/{}",
                    paymentOrderId, refunded, paymentOrder.getAmount());
            return;
        }
        // 全额：支付单 CAS SUCCESS→REFUND
        paymentOrderService.lambdaUpdate()
                .eq(PaymentOrder::getId, paymentOrderId)
                .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_SUCCESS)
                .set(PaymentOrder::getStatus, PaymentOrder.STATUS_REFUND)
                .update();

        Long orderId = paymentOrder.getOrderId();
        if (orderId == null) {
            log.warn("[退款回调] 支付单未关联订单，仅退款支付单: paymentOrderId={}", paymentOrderId);
            return;
        }
        markOrderRefunded(orderId);
        try {
            orderStockRefundService.restoreForOrder(orderId);
        } catch (Exception e) {
            log.error("[退款回调] 库存回补异常，待补偿任务兜底: orderId={}", orderId, e);
        }
        try {
            memberRewardService.reverseRewards(orderId, paymentOrder.getTenantId());
        } catch (Exception e) {
            log.error("[退款回调] 会员权益回退失败，需人工核查: orderId={}", orderId, e);
        }
    }

    /**
     * 订单仅在 已下单/配送中/已完成 时 CAS 置已退款；售后路径可能已置 6，幂等跳过。
     */
    private void markOrderRefunded(Long orderId) {
        try {
            Orders order = orderService.getById(orderId);
            if (order == null) {
                return;
            }
            Integer st = order.getStatus();
            if (st == null || Integer.valueOf(Orders.STATUS_REFUNDED).equals(st)) {
                return;
            }
            if (st == Orders.STATUS_ORDERED || st == Orders.STATUS_DELIVERING
                    || st == Orders.STATUS_COMPLETED) {
                orderService.lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, st)
                        .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                        .update();
            }
        } catch (Exception e) {
            log.error("[退款回调] 订单状态更新异常: orderId={}", orderId, e);
        }
    }
}
