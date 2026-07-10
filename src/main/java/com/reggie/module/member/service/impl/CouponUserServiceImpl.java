package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.stereotype.Service;

/**
 * 用户优惠券服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {
}
