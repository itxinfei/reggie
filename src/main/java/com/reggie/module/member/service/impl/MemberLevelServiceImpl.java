package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.MemberLevelMapper;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.MemberLevelService;
import org.springframework.stereotype.Service;

/**
 * 会员等级服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements MemberLevelService {

    @Override
    public MemberLevel getDefaultLevel() {
        LambdaQueryWrapper<MemberLevel> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(MemberLevel::getMinPoints);
        qw.last("LIMIT 1");
        return getOne(qw);
    }

    @Override
    public MemberLevel findLevelByPoints(Long points) {
        LambdaQueryWrapper<MemberLevel> qw = new LambdaQueryWrapper<>();
        qw.le(MemberLevel::getMinPoints, points);
        qw.orderByDesc(MemberLevel::getMinPoints);
        qw.last("LIMIT 1");
        return getOne(qw);
    }
}
