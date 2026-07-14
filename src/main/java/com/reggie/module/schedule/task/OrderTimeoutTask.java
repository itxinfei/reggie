package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.entity.Orders;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.report.service.ReportService;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 定时任务组件，包含订单超时自动取消、每日经营统计、库存预警等定时任务。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 原料Mapper */
    @Autowired
    private MaterialMapper materialMapper;

    /** 报表服务 */
    @Autowired
    private ReportService reportService;

    /**
     * 订单超时自动取消
     * 每5分钟执行一次，取消超过30分钟未接单的订单（status=2 待接单）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelTimeoutOrders() {
        try {
            // 超时阈值：30分钟前
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);

            LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED)
                   .lt(Orders::getOrderTime, timeoutThreshold);

            List<Orders> timeoutOrders = orderService.list(wrapper);
            if (timeoutOrders.isEmpty()) {
                return;
            }

            log.info("[定时任务] 发现{}个超时未接单订单，准备自动取消", timeoutOrders.size());

            for (Orders order : timeoutOrders) {
                try {
                    order.setStatus(Orders.STATUS_CANCELLED);
                    order.setRemark("超时未接单，系统自动取消");
                    orderService.updateById(order);
                    log.warn("[定时任务] 订单超时自动取消: orderId={}, number={}, orderTime={}",
                        order.getId(), order.getNumber(), order.getOrderTime());
                } catch (Exception e) {
                    log.error("[定时任务] 取消超时订单失败: orderId={}, error={}", order.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[定时任务] 执行订单超时取消任务异常", e);
        }
    }

    /**
     * 每日零点经营统计
     * 每天00:00执行，生成前一天的经营日报
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyStatistics() {
        try {
            String yesterday = LocalDateTime.now().minusDays(1).toLocalDate().toString();
            Long tenantId = BaseContext.getCurrentTenantId();

            if (tenantId == null) {
                log.warn("[定时任务] 每日统计跳过：无租户上下文");
                return;
            }

            Map<String, Object> report = reportService.getDailyReport(yesterday, tenantId);
            log.info("[定时任务] 每日经营统计完成: date={}, tenantId={}, report={}", yesterday, tenantId, report);
        } catch (Exception e) {
            log.error("[定时任务] 执行每日统计任务异常", e);
        }
    }

    /**
     * 库存预警检查
     * 每小时执行一次，检查食材库存是否低于预警线
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void checkInventoryAlert() {
        try {
            LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Material::getMinStock)
                   .ne(Material::getMinStock, 0);

            List<Material> materials = materialMapper.selectList(wrapper);
            if (materials.isEmpty()) {
                return;
            }

            StringBuilder alertBuilder = new StringBuilder();
            int alertCount = 0;

            for (Material material : materials) {
                if (material.getStockQty() != null
                    && material.getMinStock() != null
                    && material.getStockQty().compareTo(material.getMinStock()) <= 0) {
                    alertCount++;
                    alertBuilder.append(String.format(
                        "[%s] 当前库存: %s, 预警线: %s; ",
                        material.getName(), material.getStockQty(), material.getMinStock()
                    ));
                }
            }

            if (alertCount > 0) {
                log.warn("[定时任务] 库存预警: 共{}个食材库存不足。{}", alertCount, alertBuilder);
            }
        } catch (Exception e) {
            log.error("[定时任务] 执行库存预警检查异常", e);
        }
    }
}
