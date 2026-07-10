package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.RechargeRecordMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * 充值记录服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class RechargeRecordServiceImpl extends ServiceImpl<RechargeRecordMapper, RechargeRecord> implements RechargeRecordService {

    /** 会员服务 */
    @Autowired
    private MemberService memberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount, String paymentMethod) {
        Member member = memberService.getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }

        member.setBalance(member.getBalance().add(amount).add(giftAmount != null ? giftAmount : BigDecimal.ZERO));
        memberService.updateById(member);

        RechargeRecord record = new RechargeRecord();
        record.setMemberId(memberId);
        record.setAmount(amount);
        record.setGiftAmount(giftAmount);
        record.setPaymentMethod(paymentMethod);
        save(record);
    }
}
