package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.RiderAccount;
import com.reggie.module.delivery.model.RiderWithdrawal;

import java.math.BigDecimal;
import java.util.List;

/**
 * 骑手结算与提现服务（与会员余额解耦）。
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface RiderSettlementService {

    /**
     * 配送完成入账（幂等）。
     *
     * @param orderId      订单 ID（幂等键）
     * @param riderId      骑手 ID
     * @param tenantId     租户 ID
     * @param deliveryFee  本单配送费（元）
     */
    void settle(Long orderId, Long riderId, Long tenantId, BigDecimal deliveryFee);

    /**
     * 获取骑手账户（无账户时返回全零对象）。
     */
    RiderAccount getAccount(Long riderId, Long tenantId);

    /**
     * 申请提现（冻结可提现金额）。
     *
     * @return 提现申请
     */
    RiderWithdrawal applyWithdraw(Long riderId, Long tenantId, BigDecimal amount);

    /**
     * 骑手视角：我的提现记录。
     */
    List<RiderWithdrawal> myWithdrawals(Long riderId, Long tenantId);

    /**
     * 管理端：分页查询提现申请。
     */
    Page<RiderWithdrawal> listWithdrawals(Long tenantId, String status, int page, int size);

    /**
     * 管理端：审核提现（CAS 状态抢占，防双扣）。
     *
     * @param approve  true=通过，false=驳回
     * @return 最新提现申请
     */
    RiderWithdrawal review(Long id, boolean approve, Long reviewerId, String reviewerName, String remark);
}
