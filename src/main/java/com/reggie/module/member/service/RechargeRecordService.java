package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.RechargeRecord;
import java.math.BigDecimal;

/**
 * 充值记录服务接口
 * 管理会员余额充值记录
 *
 * @author reggie
 * @since 2026-07-09
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
}
