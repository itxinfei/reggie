package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.platform.mapper.PlatformReconcileTaskMapper;
import com.reggie.module.platform.model.PlatformReconcileTask;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformReconcileTaskService;
import com.reggie.module.platform.service.PlatformSyncService;
import com.reggie.module.platform.adapter.PlatformOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台对账任务服务实现
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Service
public class PlatformReconcileTaskServiceImpl extends ServiceImpl<PlatformReconcileTaskMapper, PlatformReconcileTask> implements PlatformReconcileTaskService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformReconcileTask reconcile(String platformType, LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户上下文缺失");
        }

        // 检查是否已存在该日期的对账任务
        PlatformReconcileTask existing = getByDate(platformType, date);
        if (existing != null) {
            log.info("对账任务已存在: platformType={}, date={}", platformType, date);
            return existing;
        }

        // 创建对账任务
        PlatformReconcileTask task = new PlatformReconcileTask();
        task.setTenantId(tenantId);
        task.setPlatformType(platformType);
        task.setReconcileDate(date);
        task.setBeginTime(LocalDateTime.of(date, LocalTime.MIDNIGHT));
        task.setEndTime(LocalDateTime.of(date, LocalTime.MIDNIGHT).plusDays(1));
        task.setStatus(0); // 进行中
        task.setCreateTime(LocalDateTime.now());
        save(task);

        try {
            // 查询平台配置
            PlatformConfig config = platformConfigService.getByPlatformType(platformType, tenantId);
            if (config == null) {
                task.setStatus(2); // 失败
                task.setErrorMessage("平台配置不存在: " + platformType);
                updateById(task);
                return task;
            }

            // 拉取平台订单
            String beginTime = task.getBeginTime().toString();
            String endTime = task.getEndTime().toString();
            List<PlatformOrder> platformOrders = platformSyncService.pullOrders(config, beginTime, endTime);

            // 查询本地订单
            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.eq(Orders::getTenantId, tenantId)
              .eq(Orders::getPlatformType, platformType)
              .between(Orders::getOrderTime, task.getBeginTime(), task.getEndTime());
            List<Orders> localOrders = orderService.list(qw);

            // 统计
            Set<String> platformOrderIds = platformOrders.stream()
                    .map(PlatformOrder::getPlatformOrderId)
                    .collect(Collectors.toSet());
            Set<String> localPlatformOrderIds = localOrders.stream()
                    .map(Orders::getPlatformOrderId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            // 匹配统计
            int matchCount = 0;
            for (String orderId : localPlatformOrderIds) {
                if (platformOrderIds.contains(orderId)) {
                    matchCount++;
                }
            }

            int missingLocalCount = platformOrderIds.size() - matchCount; // 平台有本地无
            int missingPlatformCount = localPlatformOrderIds.size() - matchCount; // 本地有平台无

            // 更新任务结果
            task.setTotalPlatformCount(platformOrders.size());
            task.setTotalLocalCount(localOrders.size());
            task.setMatchCount(matchCount);
            task.setMissingLocalCount(missingLocalCount);
            task.setMissingPlatformCount(missingPlatformCount);
            task.setStatus(1); // 完成
            task.setUpdateTime(LocalDateTime.now());
            updateById(task);

            log.info("对账完成: platformType={}, date={}, 平台={}, 本地={}, 匹配={}, 差异(平台多)={}, 差异(本地多)={}",
                    platformType, date, platformOrders.size(), localOrders.size(),
                    matchCount, missingLocalCount, missingPlatformCount);

        } catch (Exception e) {
            log.error("对账失败: platformType={}, date={}", platformType, date, e);
            task.setStatus(2); // 失败
            task.setErrorMessage(e.getMessage());
            task.setUpdateTime(LocalDateTime.now());
            updateById(task);
        }

        return task;
    }

    @Override
    public PlatformReconcileTask getByDate(String platformType, LocalDate date) {
        LambdaQueryWrapper<PlatformReconcileTask> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformReconcileTask::getTenantId, BaseContext.getCurrentTenantId())
          .eq(PlatformReconcileTask::getPlatformType, platformType)
          .eq(PlatformReconcileTask::getReconcileDate, date);
        return getOne(qw);
    }
}
