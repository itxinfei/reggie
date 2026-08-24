package com.reggie.module.platform.task;

import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformReconcileTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 平台对账定时任务
 * <p>
 * 每天凌晨自动对前一天的平台订单进行对账。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformReconcileTask {

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformReconcileTaskService reconcileTaskService;

    /**
     * 每天凌晨 2 点执行对账
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyReconcile() {
        log.info("开始执行平台对账任务");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        try {
            // 查询所有启用的平台配置
            List<PlatformConfig> configs = platformConfigService.listEnabledConfigs();
            if (configs == null || configs.isEmpty()) {
                log.info("没有启用的平台配置，跳过对账");
                return;
            }

            for (PlatformConfig config : configs) {
                try {
                    log.info("开始对账: platformType={}, date={}", config.getPlatformType(), yesterday);
                    reconcileTaskService.reconcile(config.getPlatformType(), yesterday);
                    log.info("对账完成: platformType={}", config.getPlatformType());
                } catch (Exception e) {
                    log.error("对账失败: platformType={}", config.getPlatformType(), e);
                }
            }
        } catch (Exception e) {
            log.error("执行对账任务失败", e);
        }
    }
}
