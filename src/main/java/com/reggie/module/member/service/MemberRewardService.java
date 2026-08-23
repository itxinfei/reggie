package com.reggie.module.member.service;

import com.reggie.module.order.model.Orders;

import java.math.BigDecimal;

/**
 * 会员权益结算服务
 * <p>
 * 统一处理订单成交后的会员权益发放与核销：
 * 1. 积分发放（按消费金额固定比例）
 * 2. 优惠券核销（标记已用、记录订单与用券时间）
 * 3. 储值扣减（储值支付时调用）
 * </p>
 *
 * @author reggie
 * @since 2026-08-14
 */
public interface MemberRewardService {

    /**
     * 订单成交后发放会员权益（积分 + 优惠券核销）
     * <p>通过订单 userId 关联会员，发放积分并核销本单使用的优惠券。
     * 该方法设计为可重复安全调用（订单完成/收银支付均可能触发）。</p>
     *
     * @param order 已成交订单
     */
    void grantReward(Orders order);

    /**
     * 储值扣减（储值支付时调用）
     *
     * @param userId 用户ID（用于关联会员）
     * @param amount 扣减金额
     * @return 是否扣减成功（余额不足返回 false）
     */
    boolean deductStoredBalance(Long userId, BigDecimal amount);

    /**
     * 回退订单已发放的会员权益（拒单/取消/退款时调用）
     * <p>幂等：若本单从未发放权益则直接跳过。回退范围：积分扣减、已核销优惠券恢复、储值退款。</p>
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     */
    void reverseRewards(Long orderId, Long tenantId);
}
