package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.BatchFillHelper;
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

    @Autowired
    private MemberMapper memberMapper;

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
        BatchFillHelper.fillNames(
                members,
                Member::getLevelId,
                ids -> memberLevelService.listByIds(ids).stream()
                        .collect(Collectors.toMap(MemberLevel::getId, MemberLevel::getName, (a, b) -> a)),
                Member::setLevelName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member registerByPhone(String phone, String name) {
        // 安全加固：查询手机号是否已注册时必须附加租户条件，防止跨租户探测
        Long tenantId = BaseContext.getCurrentTenantId();
        Member existing = lambdaQuery()
                .eq(Member::getPhone, phone)
                .eq(Member::getTenantId, tenantId)
                .one();
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
        // 租户归属校验：防止跨租户盗扣储值
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Member member = getById(memberId);
        if (member == null) {
            return false;
        }
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            throw new CustomException("无权操作其他租户的会员储值");
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
        // 租户归属校验：防止跨租户越权积分操作
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Member member = getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            throw new CustomException("无权操作其他租户的会员积分");
        }

        // 修改点：改用参数化 @Update（incrementPointsById），消除 setSql 字符串拼接；
        // IFNULL 防止 points 为 NULL 时整条更新无效
        baseMapper.incrementPointsById(memberId, points);

        // 重新查询更新后的会员（防并发），用于等级判断
        Member updatedMember = getById(memberId);
        if (updatedMember == null) {
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
        MemberLevel newLevel = memberLevelService.findLevelByPoints(updatedMember.getPoints());
        if (newLevel != null && (updatedMember.getLevelId() == null || !updatedMember.getLevelId().equals(newLevel.getId()))) {
            LambdaUpdateWrapper<Member> levelUpdate = new LambdaUpdateWrapper<>();
            levelUpdate.eq(Member::getId, memberId)
                    .set(Member::getLevelId, newLevel.getId())
                    .set(Member::getUpdateTime, LocalDateTime.now());
            update(levelUpdate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long memberId, int points, String bizType, Long bizId) {
        if (points <= 0) {
            return;
        }
        // 租户归属校验：防止跨租户越权积分回退
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Member member = getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            throw new CustomException("无权操作其他租户的会员积分");
        }
        // 原子扣减积分（不低于 0），避免并发回退导致积分为负
        baseMapper.decrementPointsById(memberId, points);

        // 写入一条 OUT 类型流水，便于对账与追溯
        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setType(PointsRecordType.OUT.getValue());
        record.setPoints(points);
        record.setBizType(bizType);
        record.setBizId(bizId);
        pointsRecordService.save(record);
    }

    @Override
    public Member getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        return lambdaQuery()
                .eq(Member::getUserId, userId)
                .eq(tenantId != null, Member::getTenantId, tenantId)
                .one();
    }

    @Override
    public List<Map<String, Object>> countByLevel() {
        return memberMapper.countByLevel();
    }

    @Override
    public BigDecimal calculateDiscount(Long memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        Member member = getById(memberId);
        if (member == null || member.getLevelId() == null) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        MemberLevel level = memberLevelService.getById(member.getLevelId());
        if (level == null || level.getDiscount() == null || level.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(level.getDiscount()).setScale(2, RoundingMode.HALF_UP);
    }
}
