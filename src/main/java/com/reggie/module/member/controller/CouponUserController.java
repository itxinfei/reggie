package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/coupon-user")
@Tag(name = "用户优惠券")
public class CouponUserController {

    @Autowired
    private CouponUserService couponUserService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<CouponUser>> page(int page, int pageSize, Long memberId) {
        Page<CouponUser> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(memberId != null, CouponUser::getMemberId, memberId);
        qw.orderByDesc(CouponUser::getCreatedTime);
        couponUserService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @GetMapping("/my/{memberId}")
    @Operation(summary = "我的优惠券")
    public R<List<CouponUser>> myCoupons(@PathVariable Long memberId) {
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getMemberId, memberId);
        qw.orderByDesc(CouponUser::getCreatedTime);
        List<CouponUser> list = couponUserService.list(qw);
        return R.success(list);
    }
}
