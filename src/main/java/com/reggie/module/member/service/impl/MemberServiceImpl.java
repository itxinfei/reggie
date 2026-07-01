package com.reggie.module.member.service.impl;

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

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

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
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long memberId, BigDecimal amount) {
        Member member = getById(memberId);
        if (member == null || member.getBalance().compareTo(amount) < 0) {
            return false;
        }
        member.setBalance(member.getBalance().subtract(amount));
        updateById(member);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long memberId, int points, String bizType, Long bizId) {
        Member member = getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }
        member.setPoints(member.getPoints() + points);
        updateById(member);

        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setType(PointsRecordType.IN.getValue());
        record.setPoints(points);
        record.setBizType(bizType);
        record.setBizId(bizId);
        pointsRecordService.save(record);

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
