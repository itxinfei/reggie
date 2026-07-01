package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.RechargeRecord;
import java.math.BigDecimal;

public interface RechargeRecordService extends IService<RechargeRecord> {
    void recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount, String paymentMethod);
}
