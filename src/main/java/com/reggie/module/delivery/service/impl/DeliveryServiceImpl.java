package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.enums.DeliveryOrderStatus;
import com.reggie.module.delivery.mapper.DeliveryOrderMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.model.PlatformEnum;
import com.reggie.module.delivery.platform.DeliveryPlatform;
import com.reggie.module.delivery.platform.DeliveryPlatformFactory;
import com.reggie.module.delivery.service.DeliveryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配送服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class DeliveryServiceImpl implements DeliveryService {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    /** 配送平台工厂 */
    @Autowired
    private DeliveryPlatformFactory factory;

    /** 配送订单Mapper */
    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;

    /** 状态流转白名单：每个状态允许的合法目标状态 */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new LinkedHashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(DeliveryOrderStatus.PENDING.getValue(),
                new java.util.LinkedHashSet<>(Arrays.asList(
                        DeliveryOrderStatus.ACCEPTED.getValue(),
                        DeliveryOrderStatus.CANCELLED.getValue())));
        ALLOWED_TRANSITIONS.put(DeliveryOrderStatus.ACCEPTED.getValue(),
                new java.util.LinkedHashSet<>(Arrays.asList(
                        DeliveryOrderStatus.PICKING.getValue(),
                        DeliveryOrderStatus.CANCELLED.getValue())));
        ALLOWED_TRANSITIONS.put(DeliveryOrderStatus.PICKING.getValue(),
                new java.util.LinkedHashSet<>(Arrays.asList(
                        DeliveryOrderStatus.DELIVERING.getValue(),
                        DeliveryOrderStatus.CANCELLED.getValue())));
        ALLOWED_TRANSITIONS.put(DeliveryOrderStatus.DELIVERING.getValue(),
                new java.util.LinkedHashSet<>(Arrays.asList(
                        DeliveryOrderStatus.DELIVERED.getValue(),
                        DeliveryOrderStatus.CANCELLED.getValue())));
        // DELIVERED 和 CANCELLED 是终态，不允许再流转
    }

    @Override
    public DeliveryOrder getById(String id) {
        return deliveryOrderMapper.selectById(id);
    }

    @Override
    public DeliveryOrder getByPlatformOrderId(String platformOrderId) {
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getPlatformOrderId, platformOrderId);
        qw.eq(DeliveryOrder::getTenantId, BaseContext.getCurrentTenantId());
        return deliveryOrderMapper.selectOne(qw);
    }

    @Override
    public Page<DeliveryOrder> pageOrders(int page, int pageSize, String platform, String status, String startDate, String endDate) {
        Page<DeliveryOrder> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getTenantId, BaseContext.getCurrentTenantId());
        if (StringUtils.isNotBlank(platform)) {
            qw.eq(DeliveryOrder::getPlatform, platform);
        }
        if (StringUtils.isNotBlank(status)) {
            qw.eq(DeliveryOrder::getStatus, status);
        }
        if (StringUtils.isNotBlank(startDate)) {
            qw.ge(DeliveryOrder::getOrderTime, startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            qw.le(DeliveryOrder::getOrderTime, endDateTime);
        }
        qw.orderByDesc(DeliveryOrder::getOrderTime);
        deliveryOrderMapper.selectPage(pageInfo, qw);
        return pageInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptOrder(String platform, String platformOrderId) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;

        Long tenantId = BaseContext.getCurrentTenantId();
        // 先查询订单信息（携带 tenantId 过滤条件，防止跨租户接单）
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getPlatform, platform);
        qw.eq(DeliveryOrder::getPlatformOrderId, platformOrderId);
        if (tenantId != null) {
            qw.eq(DeliveryOrder::getTenantId, tenantId);
        }
        DeliveryOrder order = deliveryOrderMapper.selectOne(qw);

        if (order == null) {
            log.warn("订单不存在: platform={}, platformOrderId={}", platform, platformOrderId);
            return false;
        }

        boolean success = dp.acceptOrder(platformOrderId);
        if (success) {
            order.setStatus(DeliveryOrderStatus.ACCEPTED.getValue());
            order.setUpdateTime(LocalDateTime.now());
            deliveryOrderMapper.updateById(order);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(Long id, String status, String remark) {
        DeliveryOrder order = deliveryOrderMapper.selectById(id);
        if (order == null) {
            log.warn("订单不存在: id={}", id);
            return false;
        }

        // 租户校验
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(order.getTenantId())) {
            log.warn("跨租户操作拒绝: id={}, currentTenant={}, orderTenant={}", id, tenantId, order.getTenantId());
            return false;
        }

        // 状态流转合法性校验
        String currentStatus = order.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(status)) {
            log.warn("非法状态流转: id={}, {} -> {}", id, currentStatus, status);
            return false;
        }

        // 同步到第三方平台（取消操作不调平台，由商家内部处理）
        if (!DeliveryOrderStatus.CANCELLED.getValue().equals(status)) {
            DeliveryPlatform dp = factory.getPlatform(order.getPlatform());
            if (dp != null) {
                dp.updateStatus(order.getPlatformOrderId(), status);
            }
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(order);
        log.info("订单状态更新: id={}, {} -> {}, 备注: {}", id, currentStatus, status, remark);
        return true;
    }

    @Override
    public Map<String, Object> getFilterOptions(String platform) {
        Map<String, Object> result = new HashMap<>();

        // 平台选项（带显示名称）
        List<Map<String, String>> platformOptions = new ArrayList<>();
        for (PlatformEnum p : PlatformEnum.values()) {
            Map<String, String> opt = new HashMap<>();
            opt.put("value", p.name());
            opt.put("label", p.getDisplayName());
            platformOptions.add(opt);
        }
        result.put("platforms", platformOptions);

        // 状态选项（带显示名称）
        List<Map<String, String>> statusOptions = new ArrayList<>();
        for (DeliveryOrderStatus s : DeliveryOrderStatus.values()) {
            Map<String, String> opt = new HashMap<>();
            opt.put("value", s.getValue());
            opt.put("label", s.getDesc());
            statusOptions.add(opt);
        }
        result.put("statuses", statusOptions);

        return result;
    }

    @Override
    public Map<String, Object> getDeliveryStats(String platform, String startDate, String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();

        // 今日订单数
        String todayStr = LocalDate.now().toString();
        LambdaQueryWrapper<DeliveryOrder> todayQw = new LambdaQueryWrapper<>();
        todayQw.eq(DeliveryOrder::getTenantId, tenantId);
        todayQw.ge(DeliveryOrder::getOrderTime, todayStr + "T00:00:00");
        todayQw.le(DeliveryOrder::getOrderTime, todayStr + "T23:59:59");
        if (StringUtils.isNotBlank(platform)) {
            todayQw.eq(DeliveryOrder::getPlatform, platform);
        }
        long todayOrders = deliveryOrderMapper.selectCount(todayQw);
        stats.put("todayOrders", todayOrders);

        // 各状态数量 + 金额汇总
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getTenantId, tenantId);
        if (StringUtils.isNotBlank(platform)) {
            qw.eq(DeliveryOrder::getPlatform, platform);
        }
        if (StringUtils.isNotBlank(startDate)) {
            qw.ge(DeliveryOrder::getOrderTime, startDate);
        }
        if (StringUtils.isNotBlank(endDate)) {
            qw.le(DeliveryOrder::getOrderTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        List<DeliveryOrder> allOrders = deliveryOrderMapper.selectList(qw);

        long pendingCount = 0, acceptedCount = 0, pickingCount = 0;
        long deliveringCount = 0, completedCount = 0, cancelledCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DeliveryOrder o : allOrders) {
            String s = o.getStatus();
            if (DeliveryOrderStatus.PENDING.getValue().equals(s)) pendingCount++;
            else if (DeliveryOrderStatus.ACCEPTED.getValue().equals(s)) acceptedCount++;
            else if (DeliveryOrderStatus.PICKING.getValue().equals(s)) pickingCount++;
            else if (DeliveryOrderStatus.DELIVERING.getValue().equals(s)) deliveringCount++;
            else if (DeliveryOrderStatus.DELIVERED.getValue().equals(s)) completedCount++;
            else if (DeliveryOrderStatus.CANCELLED.getValue().equals(s)) cancelledCount++;

            if (o.getAmount() != null) {
                totalAmount = totalAmount.add(o.getAmount());
            }
        }

        stats.put("pendingCount", pendingCount);
        stats.put("acceptedCount", acceptedCount);
        stats.put("pickingCount", pickingCount);
        stats.put("deliveringCount", deliveringCount);
        stats.put("completedCount", completedCount);
        stats.put("cancelledCount", cancelledCount);
        stats.put("totalCount", allOrders.size());
        stats.put("totalAmount", totalAmount);
        return stats;
    }

    @Override
    public boolean syncMenu(String platform, List<Map<String, Object>> dishes) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;
        return dp.syncMenu(dishes);
    }

    @Override
    public boolean syncStock(String platform, Map<Long, Integer> stock) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;
        return dp.syncStock(stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String platform, Map<String, String> params) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) {
            log.error("未知平台回调: platform={}", platform);
            return "error: unknown platform";
        }

        String type = params.getOrDefault("type", "");
        String platformOrderId = params.getOrDefault("platformOrderId", "");
        String status = params.getOrDefault("status", "");

        log.info("收到[{}]平台回调: type={}, platformOrderId={}, status={}", platform, type, platformOrderId, status);

        // 查询本地订单
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getPlatform, platform);
        qw.eq(DeliveryOrder::getPlatformOrderId, platformOrderId);
        if (tenantId != null) {
            qw.eq(DeliveryOrder::getTenantId, tenantId);
        }
        DeliveryOrder order = deliveryOrderMapper.selectOne(qw);

        switch (type) {
            case "new_order":
                // 新订单通知：保存/更新订单
                return handleNewOrderCallback(order, platform, params);

            case "status_update":
                // 状态变更通知
                if (order == null) {
                    log.warn("回调订单不存在: platform={}, platformOrderId={}", platform, platformOrderId);
                    return "error: order not found";
                }
                updateOrderStatusCallback(order, status);
                return "success";

            case "cancel":
                // 取消通知（用户或平台主动取消）
                if (order == null) {
                    log.warn("回调订单不存在: platform={}, platformOrderId={}", platform, platformOrderId);
                    return "error: order not found";
                }
                String cancelReason = params.getOrDefault("reason", "平台用户取消");
                updateOrderStatusCallback(order, DeliveryOrderStatus.CANCELLED.getValue());
                log.info("平台取消订单: platformOrderId={}, reason={}", platformOrderId, cancelReason);
                return "success";

            default:
                log.info("未处理的回调类型: type={}, params={}", type, params);
                return "ignored";
        }
    }

    /**
     * 处理新订单回调：如果本地无记录则插入，已存在则跳过（幂等）
     */
    private String handleNewOrderCallback(DeliveryOrder existOrder, String platform, Map<String, String> params) {
        if (existOrder != null) {
            log.info("订单已存在（幂等跳过）: platformOrderId={}", params.get("platformOrderId"));
            return "success";
        }

        DeliveryOrder newOrder = new DeliveryOrder();
        newOrder.setTenantId(BaseContext.getCurrentTenantId());
        newOrder.setPlatform(platform);
        newOrder.setPlatformOrderId(params.getOrDefault("platformOrderId", ""));
        newOrder.setDishSummary(params.getOrDefault("dishSummary", ""));
        String amountStr = params.getOrDefault("amount", "0");
        newOrder.setAmount(new BigDecimal(amountStr));
        newOrder.setUserName(params.getOrDefault("userName", ""));
        newOrder.setPhone(params.getOrDefault("phone", ""));
        newOrder.setAddress(params.getOrDefault("address", ""));
        newOrder.setStatus(DeliveryOrderStatus.PENDING.getValue());
        newOrder.setOrderTime(LocalDateTime.now());
        newOrder.setCreatedTime(LocalDateTime.now());
        newOrder.setUpdateTime(LocalDateTime.now());

        deliveryOrderMapper.insert(newOrder);
        log.info("新订单入库: platform={}, platformOrderId={}", platform, newOrder.getPlatformOrderId());
        return "success";
    }

    /**
     * 根据回调更新订单状态（允许任意方向的状态流转，因为平台状态是权威来源）
     */
    private void updateOrderStatusCallback(DeliveryOrder order, String status) {
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(order);
        log.info("回调更新订单状态: platformOrderId={} -> {}", order.getPlatformOrderId(), status);
    }
}
