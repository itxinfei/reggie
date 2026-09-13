package com.reggie.module.billing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.billing.dto.SubscribeDTO;
import com.reggie.module.billing.model.TenantSubscription;
import com.reggie.module.billing.service.TenantSubscriptionService;
import com.reggie.module.billing.vo.SubscriptionEntitlementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 租户订阅计费：租户发起订阅/续费、支付开通、查询权益与订阅记录；
 * 平台运营可跨租户分页查询、手动触发过期扫描（方法级 @RequiresAdmin）。
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/subscription")
@Tag(name = "SaaS计费-租户订阅", description = "订阅/续费/开通/权益/到期")
public class TenantSubscriptionController {

    @Autowired
    private TenantSubscriptionService tenantSubscriptionService;

    /**
     * 发起订阅/续费（生成待支付单）。
     *
     * @param dto 订阅请求
     * @return 待支付订阅单
     */
    @PostMapping("/subscribe")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "发起订阅/续费")
    public R<TenantSubscription> subscribe(@RequestBody @Valid SubscribeDTO dto) {
        return R.success(tenantSubscriptionService.subscribe(dto));
    }

    /**
     * 确认支付并开通（续费自动顺延到期时间）。
     *
     * @param id 订阅记录ID
     * @return 生效订阅
     */
    @PutMapping("/pay/{id}")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "确认支付并开通")
    public R<TenantSubscription> pay(@PathVariable Long id) {
        return R.success(tenantSubscriptionService.markPaid(id));
    }

    /**
     * 取消待支付订阅单。
     *
     * @param id 订阅记录ID
     * @return 结果
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消待支付订阅单")
    public R<String> cancel(@PathVariable Long id) {
        tenantSubscriptionService.cancel(id);
        return R.success("取消成功");
    }

    /**
     * 当前租户有效订阅。
     *
     * @return 有效订阅
     */
    @GetMapping("/active")
    @Operation(summary = "当前有效订阅")
    public R<TenantSubscription> active() {
        return R.success(tenantSubscriptionService.getActiveSubscription());
    }

    /**
     * 当前租户订阅权益（含剩余天数、门店/员工额度）。
     *
     * @return 权益视图
     */
    @GetMapping("/entitlement")
    @Operation(summary = "当前订阅权益")
    public R<SubscriptionEntitlementVO> entitlement() {
        return R.success(tenantSubscriptionService.getEntitlement());
    }

    /**
     * 当前租户订阅记录分页。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "我的订阅记录分页")
    public R<Page<TenantSubscription>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return R.success(tenantSubscriptionService.pageMy(page, pageSize, status));
    }

    /**
     * 平台运营跨租户分页查询（超管）。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态
     * @param tenantId 租户ID
     * @return 分页结果
     */
    @GetMapping("/admin/page")
    @RequiresAdmin
    @Operation(summary = "跨租户订阅分页（超管）")
    public R<Page<TenantSubscription>> adminPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long tenantId) {
        return R.success(tenantSubscriptionService.adminPage(page, pageSize, status, tenantId));
    }

    /**
     * 手动触发过期扫描（超管，也可由定时任务调用）。
     *
     * @return 处理条数
     */
    @PostMapping("/expire")
    @RequiresAdmin
    @Operation(summary = "扫描并置过期订阅（超管）")
    public R<Integer> expire() {
        return R.success(tenantSubscriptionService.expireOverdue());
    }
}
