package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.MemberMapper;
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

    /** 会员Mapper（用于原子加余额） */
    @Autowired
    private MemberMapper memberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount, String paymentMethod) {
        // 修改点：原子加余额（balance = balance + amount + IFNULL(giftAmount, 0)），
        // 消除 read-modify-write 并发丢失更新；先校验会员存在性
        Member member = memberService.getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }
        // 租户归属校验：防止跨租户盗充
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            throw new CustomException("无权操作其他租户的会员储值");
        }

        int rows = memberMapper.addBalance(memberId, amount, giftAmount);
        if (rows == 0) {
            throw new CustomException("会员不存在");
        }

        RechargeRecord record = new RechargeRecord();
        record.setMemberId(memberId);
        record.setAmount(amount);
        record.setGiftAmount(giftAmount);
        record.setPaymentMethod(paymentMethod);
        save(record);
    }
}
