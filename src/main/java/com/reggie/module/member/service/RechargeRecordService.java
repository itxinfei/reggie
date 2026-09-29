package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.RechargeRecord;
import java.math.BigDecimal;

/**
 * <p>
 * 充值记录服务接口
 * </p>
 * <p>管理会员余额充值记录</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface RechargeRecordService extends IService<RechargeRecord> {

    /**
     * 会员充值
     *
     * @param memberId      会员ID
     * @param amount        充值金额
     * @param giftAmount    赠送金额
     * @param paymentMethod 支付方式
     */
    void recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount, String paymentMethod);

    /**
     * C端：创建「待门店确认到账」充值单（顾客在线发起，门店收款后确认入账）。
     * 同一用户已有待确认单时直接复用，避免重复挂单。
     *
     * @param userId        C端登录用户ID
     * @param amount        充值金额
     * @param paymentMethod 意向渠道（WECHAT/ALIPAY，预留在线支付渠道）
     * @return 待确认充值记录
     */
    RechargeRecord createPendingRecharge(Long userId, BigDecimal amount, String paymentMethod);

    /**
     * 门店确认充值到账：CAS PENDING→SUCCESS 后原子加余额（同事务，先 CAS 后加钱）。
     *
     * @param rechargeNo 充值单号
     * @param employeeId 确认操作员工ID
     */
    void confirmRecharge(String rechargeNo, Long employeeId);

    /**
     * 按充值单号查询。
     *
     * @param rechargeNo 充值单号
     * @return 充值记录，不存在返回 null
     */
    RechargeRecord getByRechargeNo(String rechargeNo);

    /**
     * 在线支付成功回调：CAS PENDING→SUCCESS、回填渠道流水，然后原子加余额（本金+赠送）。
     *
     * <p>幂等口径：充值单已 SUCCESS 直接返回不重复入账；
     * 充值单已超时取消（CANCELLED）说明支付迟到，不记入账，仅告警人工退款，避免钱与余额错配。</p>
     *
     * @param rechargeId     充值记录ID
     * @param paymentTradeNo 支付单内部交易号
     * @param channelTradeNo 渠道交易号（mock 下可能为 null）
     */
    void handleRechargePaid(Long rechargeId, String paymentTradeNo, String channelTradeNo);
}
