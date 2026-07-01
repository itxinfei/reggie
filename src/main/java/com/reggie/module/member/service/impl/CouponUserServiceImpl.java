package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.stereotype.Service;

@Service
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {
}
