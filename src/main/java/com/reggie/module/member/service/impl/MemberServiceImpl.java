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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * 批量填充会员等级名称（levelName 为逻辑字段，不落库）
     * 修改点：解决前端 row.levelName 恒为 undefined 导致等级列始终显示“普通会员”的问题，
     * 由 Controller 在分页/详情查询后调用
     * @param members 会员列表
     */
    public void fillLevelName(List<Member> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        Set<Long> levelIds = members.stream()
                .map(Member::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (levelIds.isEmpty()) {
            return;
        }
        List<MemberLevel> levels = memberLevelService.listByIds(levelIds);
        Map<Long, String> nameMap = levels.stream()
                .collect(Collectors.toMap(MemberLevel::getId, MemberLevel::getName, (a, b) -> a));
        members.forEach(m -> m.setLevelName(nameMap.get(m.getLevelId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member registerByPhone(String phone, String name) {
        // 检查手机号是否已注册
        Member existing = lambdaQuery().eq(Member::getPhone, phone).one();
        if (existing != null) {
            throw new CustomException("该手机号已注册");
        }
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
        // 修改点：改用参数化 @Update（deductBalanceById），消除 setSql 字符串拼接；
        // 原子条件 balance >= #{amount} 由 SQL WHERE 保证，租户条件由 TenantLineInnerInterceptor 注入
        int rows = baseMapper.deductBalanceById(memberId, amount);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long memberId, int points, String bizType, Long bizId) {
        if (points <= 0) {
            return;
        }

        // 修改点：改用参数化 @Update（incrementPointsById），消除 setSql 字符串拼接；
        // IFNULL 防止 points 为 NULL 时整条更新无效
        baseMapper.incrementPointsById(memberId, points);

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
            // 修改点：升级等级只更新 level_id 字段，绝不整体写回（updateById 会用内存中的旧
            // balance/points 覆盖并发写入，导致余额/积分丢失更新）
            LambdaUpdateWrapper<Member> levelUpdate = new LambdaUpdateWrapper<>();
            levelUpdate.eq(Member::getId, memberId)
                    .set(Member::getLevelId, newLevel.getId())
                    .set(Member::getUpdateTime, LocalDateTime.now());
            update(levelUpdate);
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
