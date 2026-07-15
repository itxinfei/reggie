package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.enums.PointsRecordType;
import com.reggie.module.member.service.MemberLevelService;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.PointsRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    /** 会员等级服务 */
    @Autowired
    private MemberLevelService memberLevelService;

    /** 积分记录服务 */
    @Autowired
    private PointsRecordService pointsRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member registerByPhone(String phone, String name) {
        Member member = new Member();
        member.setTenantId(BaseContext.getCurrentTenantId());
        member.setPhone(phone);
        member.setName(name);
        member.setPoints(0L);
        member.setBalance(BigDecimal.ZERO);
        member.setTotalConsumption(BigDecimal.ZERO);
        member.setStatus(1);
        MemberLevel defaultLevel = memberLevelService.getDefaultLevel();
        if (defaultLevel != null) {
            member.setLevelId(defaultLevel.getId());
        }
        save(member);
        return member;
    }

    @Override
    public boolean deductBalance(Long memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // SQL 原子扣减：balance = balance - amount，WHERE balance >= amount 防止扣成负数
        LambdaUpdateWrapper<Member> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Member::getId, memberId);
        wrapper.setSql("balance = balance - CAST(" + amount.toPlainString() + " AS DECIMAL(10,2)), updated_time = NOW()");
        wrapper.apply("balance >= {0}", amount.toPlainString());
        boolean success = update(wrapper);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long memberId, int points, String bizType, Long bizId) {
        if (points <= 0) {
            return;
        }

        // 原子更新积分（IFNULL 防止 points 为 NULL 时导致整条更新无效）
        LambdaUpdateWrapper<Member> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Member::getId, memberId);
        wrapper.setSql("points = IFNULL(points, 0) + " + points + ", updated_time = NOW()");
        update(wrapper);

        // 查询更新后的积分，用于等级判断
        Member member = getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }

        // 写入积分流水
        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setType(PointsRecordType.IN.getValue());
        record.setPoints(points);
        record.setBizType(bizType);
        record.setBizId(bizId);
        pointsRecordService.save(record);

        // 检查是否升级等级
        MemberLevel newLevel = memberLevelService.findLevelByPoints(member.getPoints());
        if (newLevel != null && (member.getLevelId() == null || !member.getLevelId().equals(newLevel.getId()))) {
            member.setLevelId(newLevel.getId());
            updateById(member);
        }
    }

    @Override
    public BigDecimal calculateDiscount(Long memberId, BigDecimal amount) {
        Member member = getById(memberId);
        if (member == null || member.getLevelId() == null) {
            return amount;
        }
        MemberLevel level = memberLevelService.getById(member.getLevelId());
        if (level == null || level.getDiscount() == null) {
            return amount;
        }
        return amount.multiply(level.getDiscount()).setScale(2, RoundingMode.HALF_UP);
    }
}
