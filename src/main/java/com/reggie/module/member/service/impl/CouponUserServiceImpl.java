package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户优惠券服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {

    @Override
    public List<CouponUser> listByUserId(Long userId, Integer status) {
        LambdaQueryWrapper<CouponUser> wrapper = new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getMemberId, userId)
                .eq(CouponUser::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(CouponUser::getCreatedTime);
        if (status != null) {
            wrapper.eq(CouponUser::getStatus, status);
        }
        return this.list(wrapper);
    }

    @Override
    public void batchUpdateStatus(List<Long> couponIds, Integer status) {
        if (couponIds == null || couponIds.isEmpty()) {
            return;
        }
        this.update(new LambdaUpdateWrapper<CouponUser>()
                .in(CouponUser::getId, couponIds)
                .set(CouponUser::getStatus, status)
                .eq(CouponUser::getTenantId, BaseContext.getCurrentTenantId()));
    }
}


