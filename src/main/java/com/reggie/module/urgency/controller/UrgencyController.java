package com.reggie.module.urgency.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.urgency.service.UrgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 催单管理控制器
 * 面向中小餐厅老板，提供订单催单倒计时管理接口
 *
 * @author reggie
 * @since 2026-08-23
 */
@Slf4j
@RestController
@RequestMapping("/api/urgency")
@Tag(name = "催单管理", description = "面向餐厅老板的订单催单倒计时管理")
@RequireEmployee
public class UrgencyController {

    @Autowired
    private UrgencyService urgencyService;

    /**
     * 获取催单概览
     *
     * @return 催单统计数据
     */
    @GetMapping("/overview")
    @Operation(summary = "催单概览", description = "获取催单概览：催单中订单数/超时可催/平均等待时间/最长等待时间")
    public R<Map<String, Object>> getOverview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 获取催单概览: tenantId={}", tenantId);
        Map<String, Object> overview = urgencyService.getUrgencyOverview(tenantId);
        return R.success(overview);
    }

    /**
     * 获取催单列表
     *
     * @param status 状态筛选（可选）：COOKING-制作中, WAITING_CALL-等待叫号, COMPLETED-已完成
     * @return 催单列表
     */
    @GetMapping("/list")
    @Operation(summary = "催单列表", description = "获取催单列表，按等待时间降序排列，支持状态筛选")
    @Parameter(name = "status", description = "状态筛选（可选）：COOKING-制作中, WAITING_CALL-等待叫号, COMPLETED-已完成")
    public R<List<Map<String, Object>>> getList(@RequestParam(required = false) String status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 获取催单列表: tenantId={}, status={}", tenantId, status);
        List<Map<String, Object>> list = urgencyService.getUrgencyList(tenantId, status);
        return R.success(list);
    }

    /**
     * 催单操作
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/call/{orderId}")
    @Operation(summary = "催单", description = "对指定订单发起催单操作")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    public R<Void> callNext(@PathVariable Long orderId) {
        log.info("[催单] 催单操作: orderId={}", orderId);
        return urgencyService.callNext(orderId);
    }

    /**
     * 查看催单详情
     *
     * @param orderId 订单ID
     * @return 订单详情含制作进度
     */
    @GetMapping("/detail/{orderId}")
    @Operation(summary = "催单详情", description = "查看指定订单的催单详情，含制作进度和预估时间")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    public R<Map<String, Object>> getDetail(@PathVariable Long orderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 查看催单详情: orderId={}, tenantId={}", orderId, tenantId);
        Map<String, Object> detail = urgencyService.getUrgencyDetail(orderId, tenantId);
        return R.success(detail);
    }

    /**
     * 获取叫号排队列表
     *
     * @return 排队数据
     */
    @GetMapping("/queue")
    @Operation(summary = "叫号排队", description = "获取当前叫号信息和排队列表")
    public R<Map<String, Object>> getQueue() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 获取叫号排队: tenantId={}", tenantId);
        Map<String, Object> queue = urgencyService.getQueueList(tenantId);
        return R.success(queue);
    }

    /**
     * 获取催单统计汇总
     *
     * @return 催单统计数据
     */
    @GetMapping("/summary")
    @Operation(summary = "催单统计", description = "获取今日催单统计汇总")
    public R<Map<String, Object>> getSummary() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 获取催单统计: tenantId={}", tenantId);
        Map<String, Object> summary = urgencyService.getUrgencySummary(tenantId);
        return R.success(summary);
    }
}
