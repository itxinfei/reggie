package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequireRider;
import com.reggie.common.annotation.RequireUser;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.model.RiderEvaluation;
import com.reggie.module.delivery.service.RiderEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 骑手评价控制器：顾客提交/查询、公开列表、骑手收到的评价、管理端审核与回复。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/rider-evaluation")
@Tag(name = "骑手评价", description = "顾客对骑手配送服务的评价相关接口")
public class RiderEvaluationController {

    @Autowired
    private RiderEvaluationService riderEvaluationService;

    /**
     * 顾客提交骑手评价。
     */
    @PostMapping
    @RequireUser
    @Operation(summary = "提交骑手评价", description = "顾客对已完成的自有骑手配送订单评分，同一订单对同一骑手仅一次")
    public R<RiderEvaluation> submit(@Parameter(description = "评价信息", required = true)
                                    @Valid @RequestBody RiderEvaluation evaluation) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        RiderEvaluation result = riderEvaluationService.submit(evaluation);
        return R.success(result);
    }

    /**
     * 查询某订单对某骑手的评价（用于顾客判断是否已评价）。
     */
    @GetMapping("/order/{orderId}")
    @RequireUser
    @Operation(summary = "按订单+骑手查询评价", description = "返回该订单对该骑手的有效评价，未评价则为 null")
    public R<RiderEvaluation> getByOrder(@Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
                                        @Parameter(description = "骑手ID", required = true) @RequestParam Long riderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        return R.success(riderEvaluationService.getByOrderAndRider(tenantId, orderId, riderId));
    }

    /**
     * 顾客视角：我的骑手评价（分页）。
     */
    @GetMapping("/my")
    @RequireUser
    @Operation(summary = "我的骑手评价", description = "分页查询当前用户提交过的骑手评价")
    public R<Page<RiderEvaluation>> my(@Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        if (tenantId == null || userId == null) {
            return R.error("请先登录");
        }
        return R.success(riderEvaluationService.pageByUserId(tenantId, userId, page, PageUtils.cap(pageSize)));
    }

    /**
     * 公开视角：某骑手的评价列表（仅已通过）。
     */
    @GetMapping("/rider/{riderId}")
    @Operation(summary = "骑手评价列表", description = "分页查询指定骑手已通过的评价")
    public R<Page<RiderEvaluation>> listByRider(@Parameter(description = "骑手ID", required = true) @PathVariable Long riderId,
                                                @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        return R.success(riderEvaluationService.pageByRiderId(tenantId, riderId, page, PageUtils.cap(pageSize)));
    }

    /**
     * 骑手评分统计（平均分 + 数量）。
     */
    @GetMapping("/rider/{riderId}/stats")
    @Operation(summary = "骑手评分统计", description = "返回指定骑手的平均分与评价数量")
    public R<Map<String, Object>> stats(@Parameter(description = "骑手ID", required = true) @PathVariable Long riderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        return R.success(riderEvaluationService.getRiderStats(tenantId, riderId));
    }

    /**
     * 骑手视角：收到的评价（当前登录骑手）。
     */
    @GetMapping("/received")
    @RequireRider
    @Operation(summary = "骑手收到的评价", description = "分页查询当前登录骑手收到的评价")
    public R<Page<RiderEvaluation>> received(@Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                             @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long riderId = BaseContext.getCurrentId();
        if (tenantId == null || riderId == null) {
            return R.error("请先登录");
        }
        return R.success(riderEvaluationService.pageReceived(tenantId, riderId, page, PageUtils.cap(pageSize)));
    }

    /**
     * 管理端：骑手评价分页（支持骑手姓名/评分/状态筛选）。
     */
    @GetMapping("/admin/page")
    @RequireEmployee
    @Operation(summary = "管理端骑手评价分页", description = "支持按骑手姓名、评分、状态筛选，需员工权限")
    public R<Page<RiderEvaluation>> adminPage(
            @Parameter(description = "骑手姓名（模糊）") @RequestParam(required = false) String riderName,
            @Parameter(description = "评分（1-5）") @RequestParam(required = false) Integer starRating,
            @Parameter(description = "状态（0待审1通过2拒绝）") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        return R.success(riderEvaluationService.adminPage(tenantId, riderName, starRating, status, page, PageUtils.cap(pageSize)));
    }

    /**
     * 管理端：回复评价。
     */
    @PutMapping("/{id}/reply")
    @RequireEmployee
    @Operation(summary = "回复骑手评价", description = "商家/管理员回复评价，需员工权限")
    public R<String> reply(@Parameter(description = "评价ID", required = true) @PathVariable Long id,
                           @Parameter(description = "回复内容", required = true) @RequestBody Map<String, String> params) {
        String replyContent = params.get("replyContent");
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        boolean ok = riderEvaluationService.reply(id, replyContent, BaseContext.getCurrentId(), tenantId);
        return ok ? R.success("回复成功") : R.error("回复失败");
    }
}
