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
}
