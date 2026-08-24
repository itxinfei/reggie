package com.reggie.module.platform.task;

import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 平台订单拉取定时任务
 * <p>
 * 定时拉取各平台订单，通过 PlatformSyncService 统一编排。
 * 复用项目现有的 @Scheduled 调度框架。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformPullTask {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PlatformConfigService configService;

    @Autowired
    private PlatformSyncService syncService;

    /**
     * 每 30 秒拉取一次启用平台的订单
     * 注意：实际拉单间隔应通过配置中心或数据库配置动态调整
     */
    @Scheduled(fixedDelay = 30000)
    public void pullAllEnabledPlatformOrders() {
        List<PlatformConfig> enabledConfigs = configService.listEnabledConfigs();
        if (enabledConfigs == null || enabledConfigs.isEmpty()) {
            return;
        }

        String endTime = LocalDateTime.now().format(FORMATTER);
        String beginTime = LocalDateTime.now().minusMinutes(5).format(FORMATTER);

        for (PlatformConfig config : enabledConfigs) {
            try {
                log.info("[平台拉单] 开始拉单: platformType={}, shopId={}",
                        config.getPlatformType(), config.getShopId());
                syncService.pullOrders(config, beginTime, endTime);
                log.info("[平台拉单] 完成拉单: platformType={}", config.getPlatformType());
            } catch (Exception e) {
                log.error("[平台拉单] 失败: platformType={}, shopId={}",
                        config.getPlatformType(), config.getShopId(), e);
            }
        }
    }
}
