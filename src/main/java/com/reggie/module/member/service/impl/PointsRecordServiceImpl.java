package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.member.mapper.PointsRecordMapper;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.PointsRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 积分记录服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {

    @Override
    public Page<PointsRecord> listByMember(Long memberId, int page, int pageSize) {
        Page<PointsRecord> pageRequest = new Page<>(page, pageSize);
        return this.page(pageRequest,
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getMemberId, memberId)
                        .eq(PointsRecord::getTenantId, BaseContext.getCurrentTenantId())
                        .orderByDesc(PointsRecord::getCreatedTime));
    }

    @Override
    public int getBalance(Long memberId) {
        List<PointsRecord> records = this.list(new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getMemberId, memberId)
                        .eq(PointsRecord::getTenantId, BaseContext.getCurrentTenantId()));
        return records.stream()
                .mapToInt(PointsRecord::getPoints)
                .sum();
    }
}
