package com.reggie.module.member.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.enums.CouponStatus;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.CouponAvailableDTO;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.CouponUserService;
import com.reggie.module.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * C 端（用户端）优惠券 Controller。
 * <p>
 * 与 {@link CouponTemplateController} / {@link CouponUserController} 区分：
 * 后者类级 {@code @RequireEmployee}，仅供后台/收银台员工调用；
 * 本 Controller 无 {@code @RequireEmployee}，供已登录 C 端用户在会员中心、下单页使用，
 * 通过 {@link BaseContext#getCurrentId()} 绑定当前登录用户，避免请求方伪造 memberId。
 * </p>
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@RestController
@RequestMapping("/front/coupon")
@Tag(name = "C端优惠券", description = "C端用户在会员中心/下单页使用的优惠券接口")
public class FrontCouponController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Autowired
    private CouponUserService couponUserService;

    /**
     * 获取当前登录用户关联的会员。
     * 会员体系里 user 与 member 是 1:1，未开通会员时返回 401 由调用方提示注册。
     */
    private Member currentMember() {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (userId == null) {
            throw new RuntimeException("NOTLOGIN");
        }
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        qw.eq(Member::getUserId, userId);
        qw.eq(Member::getTenantId, tenantId);
        Member member = memberService.getOne(qw);
        if (member == null) {
            return null;
        }
        return member;
    }

    @GetMapping("/my")
    @Operation(summary = "我的优惠券", description = "查询当前登录用户已领取的全部优惠券")
    public R<List<CouponUser>> myCoupons() {
        Member member = currentMember();
        if (member == null) {
            return R.error("尚未开通会员，请先注册会员");
        }
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getMemberId, member.getId());
        qw.eq(CouponUser::getTenantId, BaseContext.getCurrentTenantId());
        qw.orderByDesc(CouponUser::getCreatedTime);
        List<CouponUser> list = couponUserService.list(qw);
        return R.success(list);
    }

    @GetMapping("/available")
    @Operation(summary = "可领券列表", description = "当前租户下启用且有剩余库存的优惠券模板")
    public R<List<CouponTemplate>> availableTemplates() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponTemplate::getTenantId, tenantId);
        qw.eq(CouponTemplate::getStatus, 1);
        qw.gt(CouponTemplate::getRemainCount, 0);
        qw.orderByDesc(CouponTemplate::getCreatedTime);
        List<CouponTemplate> list = couponTemplateService.list(qw);
        return R.success(list);
    }

    @PostMapping("/claim/{templateId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "领取优惠券", description = "当前登录用户领取指定优惠券模板，从 session 绑定会员，不信任请求方传入的 memberId")
    public R<String> claim(@PathVariable Long templateId) {
        Member member = currentMember();
        if (member == null) {
            return R.error("尚未开通会员，请先注册会员");
        }
        boolean ok = couponTemplateService.claimCoupon(member.getId(), templateId);
        if (ok) {
            return R.success("领取成功");
        }
        return R.error("领取失败，优惠券不可用或已领完");
    }

    /**
     * 下单可用券：按订单金额筛出当前用户可抵扣的未使用且未过期优惠券。
     *
     * @param orderAmount 订单金额（不含运费、不含优惠）
     * @return 可用券列表（含可抵扣金额）
     */
    @GetMapping("/usable")
    @Operation(summary = "下单可用券", description = "按订单金额筛出当前用户可抵扣的未使用优惠券")
    public R<List<CouponAvailableDTO>> usableCoupons(@RequestParam BigDecimal orderAmount) {
        Member member = currentMember();
        if (member == null) {
            return R.error("尚未开通会员，请先注册会员");
        }
        List<CouponAvailableDTO> items = couponUserService
                .availableCoupons(member.getId(), orderAmount);
        return R.success(items);
    }

    @GetMapping("/check-expired")
    @Operation(summary = "检查过期", description = "返回当前用户已过期未清理的券数量（用于前端提示）")
    public R<Integer> expiredCount() {
        Member member = currentMember();
        if (member == null) {
            return R.success(0);
        }
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getMemberId, member.getId());
        qw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        qw.lt(CouponUser::getExpireTime, LocalDateTime.now());
        Long cnt = couponUserService.count(qw);
        return R.success(cnt != null ? cnt.intValue() : 0);
    }

    /**
     * 下单可用券视图：复用 CouponUserService.availableCoupons 返回的
     * CouponAvailableDTO（含券 ID、模板 ID、模板名、可抵扣金额、条件金额、状态、过期时间）。
     */
}