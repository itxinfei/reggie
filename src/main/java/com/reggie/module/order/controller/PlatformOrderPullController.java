package com.reggie.module.order.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台订单即时拉取接口
 * <p>
 * 提供给后台手动触发「从外卖平台拉单」操作（定时任务之外的补充手段），
 * 拉到的新订单会自动落库（见 PlatformOrderPersistService 幂等去重）。
 * 仅员工会话可调用。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@RestController
@RequestMapping("/api/platform")
@Tag(name = "平台订单拉取", description = "从外卖平台手动拉单并落库")
public class PlatformOrderPullController {

    private final PlatformConfigService configService;
    private final PlatformSyncService syncService;

    @Autowired
    public PlatformOrderPullController(PlatformConfigService configService, PlatformSyncService syncService) {
        this.configService = configService;
        this.syncService = syncService;
    }

    /**
     * 即时拉取指定平台的订单
     *
     * @param platformType 平台类型 MEITUAN / ELEME / DOUYIN
     * @param minutes      回溯时间窗（分钟），默认 30
     * @return 拉取数与新增落库数
     */
    @RequireEmployee
    @GetMapping("/pull")
    @Operation(summary = "从外卖平台拉单并落库")
    public R<Map<String, Object>> pull(@Parameter(description = "平台类型（MEITUAN/ELEME/DOUYIN）", required = true) @RequestParam String platformType,
                                       @Parameter(description = "回溯时间窗（分钟），默认30", required = true) @RequestParam(defaultValue = "30") int minutes) {
        if (BaseContext.getCurrentTenantId() == null) {
            return R.error("缺少租户上下文");
        }
        PlatformConfig config = configService.getByPlatformType(platformType, BaseContext.getCurrentTenantId());
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return R.error("未找到启用的平台配置: " + platformType);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String endTime = LocalDateTime.now().format(fmt);
        String beginTime = LocalDateTime.now().minusMinutes(minutes).format(fmt);

        List<PlatformOrder> orders = syncService.pullOrders(config, beginTime, endTime);
        int inserted = syncService.persistOrders(config, orders);
        Map<String, Object> result = new HashMap<>(4);
        result.put("platformType", platformType);
        result.put("pulled", orders.size());
        result.put("inserted", inserted);
        return R.success(result);
    }

    /**
     * 回传订单状态到外卖平台（接单/拒单/出餐/完成/取消）
     *
     * @param platformType     平台类型 MEITUAN / ELEME / DOUYIN
     * @param platformOrderId  平台订单号
     * @param action           动作 accept/reject/prepare/complete/cancel
     * @return 操作结果
     */
    @RequireEmployee
    @PostMapping("/pushStatus")
    @Operation(summary = "回传订单状态到外卖平台")
    public R<Void> pushStatus(@Parameter(description = "平台类型（MEITUAN/ELEME/DOUYIN）", required = true) @RequestParam String platformType,
                              @Parameter(description = "平台订单号", required = true) @RequestParam String platformOrderId,
                              @Parameter(description = "动作（accept-接单/reject-拒单/prepare-出餐/complete-完成/cancel-取消）", required = true) @RequestParam String action) {
        if (BaseContext.getCurrentTenantId() == null) {
            return R.error("缺少租户上下文");
        }
        PlatformConfig config = configService.getByPlatformType(platformType, BaseContext.getCurrentTenantId());
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return R.error("未找到启用的平台配置: " + platformType);
        }
        try {
            syncService.pushOrderStatus(config, platformOrderId, action);
            return R.success(null);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("[平台回传] 状态回传失败: platformType={}, orderId={}, action={}",
                    platformType, platformOrderId, action, e);
            return R.error("状态回传失败：" + e.getMessage());
        }
    }
}
