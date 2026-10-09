package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import com.reggie.module.urgency.service.UrgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 未接单订单定时扫描任务（漏单预警核心调度）
 * </p>
 * <p>
 * 每 30 秒扫描各活跃租户的"待接单"订单：超黄金时长（默认 3 分钟）未接单即主动通知店长，
 * 超漏单阈值（默认 3 倍黄金时长）升级为漏单告警。与接单大屏（pending-monitor.html）形成
 * "大屏可见 + 主动告警"双通道，确保商家在顾客催单之前就感知到漏单风险。
 * </p>
 * <p>
 * 与 {@link OrderTimeoutTask} 相同模式：Redis 分布式锁防多实例重复执行，
 * 无 HTTP 请求上下文，需通过遍历活跃租户列表设置 ThreadLocal。
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Slf4j
@Component
public class UnacceptedOrderScanTask {

    /** 扫描间隔（毫秒）：30 秒 */
    private static final long SCAN_INTERVAL_MS = 30 * 1000L;

    /** 分布式锁过期时间（秒） */
    private static final long LOCK_TTL_SECONDS = 20L;

    @Autowired
    private UrgencyService urgencyService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RedisLockUtil redisLockUtil;

    /**
     * 每 30 秒扫描未接单订单并主动告警
     */
    @Scheduled(fixedRate = SCAN_INTERVAL_MS)
    public void scanUnacceptedOrders() {
        // 分布式锁防多实例重复告警（fail-closed：拿不到锁=另一实例在执行，跳过本轮）
        redisLockUtil.executeWithLock("schedule:lock:unaccepted-scan", LOCK_TTL_SECONDS, () -> {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }
            int totalAlerted = 0;
            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    totalAlerted += urgencyService.scanUnacceptedAndAlert(tenant.getId());
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.error("[漏单预警] 租户 {} 扫描失败: error={}", tenant.getId(), e.getMessage());
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalAlerted > 0) {
                log.info("[漏单预警] 未接单扫描完成，共处理 {} 个租户，本轮新增通知 {} 条", tenants.size(), totalAlerted);
            }
        });
    }
}
