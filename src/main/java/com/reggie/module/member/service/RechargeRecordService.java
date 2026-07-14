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
}
