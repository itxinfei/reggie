package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.module.delivery.mapper.DeliveryOrderMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.enums.DeliveryOrderStatus;
import com.reggie.module.delivery.platform.DeliveryPlatform;
import com.reggie.module.delivery.platform.DeliveryPlatformFactory;
import com.reggie.module.delivery.service.DeliveryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

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

    @Override
    public DeliveryOrder getById(String id) {
        return deliveryOrderMapper.selectById(id);
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
            order.setUpdatedTime(LocalDateTime.now());
            deliveryOrderMapper.updateById(order);
        }
        return success;
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
    public String handleCallback(String platform, Map<String, String> params) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return "error: unknown platform";
        log.info("Received callback from {}: {}", platform, params);
        return "success";
    }
}
