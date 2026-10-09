package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
import com.reggie.enums.PointsRecordType;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.mapper.PointsRecordMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.MemberLevelService;
import com.reggie.module.member.service.PointsRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分记录服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements
        PointsRecordService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberLevelService memberLevelService;

    /**
     * 查询列表 by member。
     * @param memberId 参数 memberId
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @return 返回结果
     */
    @Override
    public Page<PointsRecord> listByMember(Long memberId, int page, int pageSize) {
        Page<PointsRecord> pageRequest = PageUtils.of(page, pageSize);
        return this.page(pageRequest,
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getMemberId, memberId)
                        .eq(PointsRecord::getTenantId, BaseContext.getCurrentTenantId())
                        .orderByDesc(PointsRecord::getCreatedTime));
    }

    /**
     * 处理过期积分：按 member_id 分组汇总过期积分，逐个扣减并写入 OUT 流水，
     * 然后将过期记录逻辑删除防止重复处理。
     * 由 PointsExpireTask 每天凌晨调用，调用前需设置 BaseContext.currentTenantId。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expirePointsBatch() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 查询当前租户下所有过期积分，按 member_id 分组汇总
        List<Map<String, Object>> expiredGroups = baseMapper.sumExpiredByMemberId(now, tenantId);
        if (expiredGroups == null || expiredGroups.isEmpty()) {
            return;
        }

        int totalExpiredMembers = 0;
        int totalExpiredPoints = 0;

        for (Map<String, Object> group : expiredGroups) {
            Long memberId = ((Number) group.get("memberId")).longValue();
            int expiredPoints = ((Number) group.get("expiredPoints")).intValue();
            if (expiredPoints <= 0) {
                continue;
            }

            // 2. 原子扣减积分（不低于0）
            int rows = memberMapper.decrementPointsById(memberId, expiredPoints);
            if (rows <= 0) {
                log.warn("[积分过期] 扣减失败，会员不存在: memberId={}", memberId);
                continue;
            }

            // 3. 写入 OUT 流水记录（remark 标记为过期扣减）
            PointsRecord outRecord = new PointsRecord();
            outRecord.setMemberId(memberId);
            outRecord.setType(PointsRecordType.OUT.getValue());
            outRecord.setPoints(expiredPoints);
            outRecord.setBizType("points_expire");
            outRecord.setRemark("积分过期自动扣减");
            save(outRecord);

            // 4. 扣减后检查是否需要降级
            Member member = memberMapper.selectById(memberId);
            if (member != null && member.getLevelId() != null) {
                MemberLevel matchedLevel = memberLevelService.findLevelByPoints(member.getPoints());
                if (matchedLevel != null && !matchedLevel.getId().equals(member.getLevelId())) {
                    LambdaUpdateWrapper<Member> levelUpdate = new LambdaUpdateWrapper<>();
                    levelUpdate.eq(Member::getId, memberId)
                            .eq(Member::getLevelId, member.getLevelId())
                            .set(Member::getLevelId, matchedLevel.getId());
                    boolean updated = memberMapper.update(null, levelUpdate) > 0;
                    log.info("[积分过期] 会员降级: memberId={}, oldLevel={}, newLevel={}, updated={}",
                            memberId, member.getLevelId(), matchedLevel.getId(), updated);
                }
            }

            totalExpiredMembers++;
            totalExpiredPoints += expiredPoints;
        }

        // 5. 将过期的 IN 记录逻辑删除，防止下次重复处理
        int deletedRows = baseMapper.markExpiredRecordsDeleted(now, tenantId);
        log.info("[积分过期] 租户{}处理完成: 影响会员{}人, 过期积分{}分, 标记记录{}条",
                tenantId, totalExpiredMembers, totalExpiredPoints, deletedRows);
    }
}
