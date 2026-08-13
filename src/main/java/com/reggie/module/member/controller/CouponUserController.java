package com.reggie.module.member.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户优惠券管理控制器
 * 提供用户优惠券的查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/api/member/coupon-user")
@Tag(name = "用户优惠券")
public class CouponUserController {

    @Autowired
    private CouponUserService couponUserService;

    /**
     * 分页查询用户优惠券列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param memberId 会员ID（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询用户优惠券列表，支持按会员ID筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "memberId", description = "会员ID（可选）")
    public R<Page<CouponUser>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, Long memberId) {
        Page<CouponUser> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(memberId != null, CouponUser::getMemberId, memberId);
        qw.orderByDesc(CouponUser::getCreatedTime);
        couponUserService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 查询指定会员的所有优惠券
     * @param memberId 会员ID
     * @return 优惠券列表
     */
    @GetMapping("/my/{memberId}")
    @Operation(summary = "我的优惠券", description = "查询指定会员的所有优惠券")
    @Parameter(name = "memberId", description = "会员ID", required = true)
    public R<List<CouponUser>> myCoupons(@PathVariable Long memberId) {
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getMemberId, memberId);
        qw.orderByDesc(CouponUser::getCreatedTime);
        List<CouponUser> list = couponUserService.list(qw);
        return R.success(list);
    }
}

