package com.reggie.module.inventory.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.service.ReplenishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 智能补货控制器
 * <p>
 * 面向中小餐厅老板，提供一键查看需补食材、补货量和紧急度的聚合数据。
 * 补货建议基于加权日均消耗算法（近7天权重1.5），Redis 缓存10分钟。
 * </p>
 *
 * @author reggie
 * @since 2026-08-23
 */
@RestController
@RequestMapping("/api/inventory/replenish")
@Slf4j
@RequireEmployee
@Tag(name = "智能补货", description = "面向中小餐厅老板的智能补货建议与看板")
public class ReplenishController {

    @Autowired
    private ReplenishService replenishService;

    /**
     * 获取智能补货看板数据
     * <p>
     * 汇总返回：紧急/紧迫/关注食材计数、补货总金额预估、断货TOP5、补货建议列表
     * Redis: String key=inventory:replenish:suggest:{tenantId}, TTL=10min
     */
    @GetMapping("/dashboard")
    @Operation(summary = "补货看板", description = "获取今日需补食材的汇总看板数据（紧急度分布、总金额、TOP5断货预警）")
    public R<Map<String, Object>> dashboard() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[智能补货] 获取补货看板 tenantId={}", tenantId);
        Map<String, Object> data = replenishService.getReplenishDashboard(tenantId);
        return R.success(data);
    }

    /**
     * 获取智能补货建议列表
     * <p>
     * 加权日均消耗（近7天权重1.5）+ 安全库存 + 补货周期 - 当前库存
     * 只返回建议采购量 > 0 的食材
     */
    @GetMapping("/suggest")
    @Operation(summary = "补货建议", description = "获取智能补货建议列表，支持自定义统计天数和补货周期")
    public R<List<Map<String, Object>>> suggest(
            @Parameter(name = "days", description = "统计天数", example = "30")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(name = "replenishCycle", description = "补货周期天数", example = "14")
            @RequestParam(defaultValue = "14") int replenishCycle) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[智能补货] 获取补货建议 tenantId={} days={} cycle={}", tenantId, days, replenishCycle);
        List<Map<String, Object>> data = replenishService.getSmartReplenishSuggest(tenantId, days, replenishCycle);
        return R.success(data);
    }
}