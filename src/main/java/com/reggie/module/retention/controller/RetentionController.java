package com.reggie.module.retention.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.retention.service.RetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会员留存自动化 Controller
 *
 * @author reggie
 * @since 2026-08-23
 */
@RestController
@RequestMapping("/api/retention")
@Tag(name = "会员留存自动化")
@RequireEmployee
@Slf4j
public class RetentionController {

    @Autowired
    private RetentionService retentionService;

    @GetMapping("/overview")
    @Operation(summary = "会员留存概览")
    public R<Map<String, Object>> getRetentionOverview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> overview = retentionService.getRetentionOverview(tenantId);
        return R.success(overview);
    }

    @GetMapping("/list")
    @Operation(summary = "会员留存列表")
    public R<List<Map<String, Object>>> getMemberList(
            @Parameter(description = "等级筛选（GOLD/SILVER/NORMAL）") @RequestParam(required = false) String level,
            @Parameter(description = "状态筛选（ACTIVE/DORMANT/CHURNED）") @RequestParam(required = false) String status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> list = retentionService.getMemberList(tenantId, level, status);
        return R.success(list);
    }

    @GetMapping("/ranking")
    @Operation(summary = "积分排行榜")
    public R<List<Map<String, Object>>> getPointsRanking() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = retentionService.getPointsRanking(tenantId);
        return R.success(ranking);
    }

    @GetMapping("/warning")
    @Operation(summary = "流失预警会员")
    public R<List<Map<String, Object>>> getChurnWarning() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> warning = retentionService.getChurnWarning(tenantId);
        return R.success(warning);
    }

    @GetMapping("/recommend")
    @Operation(summary = "智能券推荐")
    public R<List<Map<String, Object>>> getSmartRecommend() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> recommend = retentionService.getSmartRecommend(tenantId);
        return R.success(recommend);
    }

    @PostMapping("/send")
    @Operation(summary = "定向发券")
    public R<Void> sendCoupon(
            @Parameter(description = "会员ID") @RequestParam Long memberId) {
        log.info("Send coupon to member: {}", memberId);
        return retentionService.sendCoupon(memberId);
    }

    @PostMapping("/send-batch")
    @Operation(summary = "批量发券")
    public R<Void> batchSendCoupon(
            @Parameter(description = "会员ID列表") @RequestBody List<Long> memberIds) {
        log.info("Batch send coupon to {} members", memberIds != null ? memberIds.size() : 0);
        return retentionService.batchSendCoupon(memberIds);
    }
}
