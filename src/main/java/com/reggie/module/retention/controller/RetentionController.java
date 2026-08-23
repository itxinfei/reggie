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
@Tag(name = "Membership Retention Automation")
@RequireEmployee
@Slf4j
public class RetentionController {

    @Autowired
    private RetentionService retentionService;

    @GetMapping("/overview")
    @Operation(summary = "Get retention overview with tiered statistics")
    public R<Map<String, Object>> getRetentionOverview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> overview = retentionService.getRetentionOverview(tenantId);
        return R.success(overview);
    }

    @GetMapping("/list")
    @Operation(summary = "Get member list with optional filters")
    public R<List<Map<String, Object>>> getMemberList(
            @Parameter(description = "Level filter (GOLD/SILVER/NORMAL)") @RequestParam(required = false) String level,
            @Parameter(description = "Status filter (ACTIVE/DORMANT/CHURNED)") @RequestParam(required = false) String status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> list = retentionService.getMemberList(tenantId, level, status);
        return R.success(list);
    }

    @GetMapping("/ranking")
    @Operation(summary = "Get points ranking top 20")
    public R<List<Map<String, Object>>> getPointsRanking() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = retentionService.getPointsRanking(tenantId);
        return R.success(ranking);
    }

    @GetMapping("/warning")
    @Operation(summary = "Get churn warning members (>30 days without order)")
    public R<List<Map<String, Object>>> getChurnWarning() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> warning = retentionService.getChurnWarning(tenantId);
        return R.success(warning);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Get smart coupon recommendation")
    public R<List<Map<String, Object>>> getSmartRecommend() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> recommend = retentionService.getSmartRecommend(tenantId);
        return R.success(recommend);
    }

    @PostMapping("/send")
    @Operation(summary = "Send coupon to a specific member")
    public R<Void> sendCoupon(
            @Parameter(description = "Member ID") @RequestParam Long memberId) {
        log.info("Send coupon to member: {}", memberId);
        return retentionService.sendCoupon(memberId);
    }

    @PostMapping("/send-batch")
    @Operation(summary = "Batch send coupons to members")
    public R<Void> batchSendCoupon(
            @Parameter(description = "Member ID list") @RequestBody List<Long> memberIds) {
        log.info("Batch send coupon to {} members", memberIds != null ? memberIds.size() : 0);
        return retentionService.batchSendCoupon(memberIds);
    }
}
