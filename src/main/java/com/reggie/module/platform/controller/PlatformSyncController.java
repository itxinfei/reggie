package com.reggie.module.platform.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.platform.model.DishPlatformMapping;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.DishPlatformMappingService;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台同步触发接口
 * <p>
 * 提供给后台手动触发「商品上/下架、库存、营业状态」同步到外卖平台。
 * 仅员工会话可调用。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@RestController
@RequestMapping("/api/platform")
@RequireEmployee
@Tag(name = "平台同步触发", description = "手动触发商品/库存/营业状态同步到外卖平台")
public class PlatformSyncController {

    private final PlatformConfigService configService;
    private final PlatformSyncService syncService;
    private final DishPlatformMappingService mappingService;

    @Autowired
    public PlatformSyncController(PlatformConfigService configService,
                                  PlatformSyncService syncService,
                                  DishPlatformMappingService mappingService) {
        this.configService = configService;
        this.syncService = syncService;
        this.mappingService = mappingService;
    }

    /**
     * 同步菜品上/下架到平台
     *
     * @param platformType 平台类型
     * @param dishId       本系统菜品 ID
     * @param action       on_shelf / off_shelf
     */
    @PostMapping("/syncDish")
    @Operation(summary = "同步菜品上/下架到外卖平台")
    public R<Void> syncDish(@RequestParam String platformType,
                            @RequestParam Long dishId,
                            @RequestParam String action) {
        PlatformConfig config = resolveConfig(platformType);
        List<DishPlatformMapping> mappings = mappingService.listByDishIdAndPlatformType(dishId, platformType);
        DishPlatformMapping mapping = (mappings != null && !mappings.isEmpty()) ? mappings.get(0) : null;
        if (mapping == null || mapping.getPlatformDishId() == null) {
            return R.error("未找到该菜品在 " + platformType + " 的平台映射");
        }
        try {
            syncService.syncDish(config, dishId, mapping.getPlatformDishId(), action);
            return R.success(null);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("[平台同步] 菜品同步失败: dishId={}, action={}", dishId, action, e);
            return R.error("菜品同步失败：" + e.getMessage());
        }
    }

    /**
     * 同步库存到平台
     *
     * @param platformType   平台类型
     * @param platformDishId 平台菜品 ID
     * @param remainQty      剩余可售数
     */
    @PostMapping("/syncStock")
    @Operation(summary = "同步库存到外卖平台")
    public R<Void> syncStock(@RequestParam String platformType,
                             @RequestParam String platformDishId,
                             @RequestParam int remainQty) {
        PlatformConfig config = resolveConfig(platformType);
        try {
            syncService.syncStock(config, platformDishId, remainQty);
            return R.success(null);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("[平台同步] 库存同步失败: platformDishId={}", platformDishId, e);
            return R.error("库存同步失败：" + e.getMessage());
        }
    }

    /**
     * 同步营业状态到平台
     *
     * @param platformType 平台类型
     * @param open         是否营业
     */
    @PostMapping("/syncBusiness")
    @Operation(summary = "同步营业状态到外卖平台")
    public R<Void> syncBusiness(@RequestParam String platformType,
                                @RequestParam boolean open) {
        PlatformConfig config = resolveConfig(platformType);
        try {
            syncService.syncBusinessStatus(config, open);
            return R.success(null);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("[平台同步] 营业状态同步失败: platformType={}, open={}", platformType, open, e);
            return R.error("营业状态同步失败：" + e.getMessage());
        }
    }

    /** 解析并校验当前租户下启用的平台配置 */
    private PlatformConfig resolveConfig(String platformType) {
        if (BaseContext.getCurrentTenantId() == null) {
            throw new CustomException("缺少租户上下文");
        }
        PlatformConfig config = configService.getByPlatformType(platformType, BaseContext.getCurrentTenantId());
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new CustomException("未找到启用的平台配置: " + platformType);
        }
        return config;
    }
}
