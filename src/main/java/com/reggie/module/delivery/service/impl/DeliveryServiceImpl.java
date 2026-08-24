package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
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

    /** 平台回调类型：新订单 */
    private static final String CALLBACK_TYPE_NEW_ORDER = "new_order";
    /** 平台回调类型：状态更新 */
    private static final String CALLBACK_TYPE_STATUS_UPDATE = "status_update";
    /** 平台回调类型：取消 */
    private static final String CALLBACK_TYPE_CANCEL = "cancel";

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
        // 修复 IDOR：按 id + tenantId 条件查询，防止跨租户访问
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return null;
        }
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getId, id)
          .eq(DeliveryOrder::getTenantId, tenantId);
        return deliveryOrderMapper.selectOne(qw);
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
        Page<DeliveryOrder> pageInfo = PageUtils.of(page, pageSize);
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

        // fail-closed：强制租户校验，防止跨租户接单
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            log.warn("接单失败：租户上下文缺失");
            return false;
        }
        LambdaQueryWrapper<DeliveryOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryOrder::getPlatform, platform);
        qw.eq(DeliveryOrder::getPlatformOrderId, platformOrderId);
        qw.eq(DeliveryOrder::getTenantId, tenantId);
        DeliveryOrder order = deliveryOrderMapper.selectOne(qw.last("LIMIT 1"));

        if (order == null) {
            log.warn("订单不存在: platform={}, platformOrderId={}", platform, platformOrderId);
            return false;
        }

        String currentStatus = order.getStatus();
        // #46 状态机校验：仅当当前状态为 PENDING 时允许接单，禁止对终态/中间态订单接单
        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(DeliveryOrderStatus.ACCEPTED.getValue())) {
            log.warn("接单失败，订单当前状态不允许接单: platformOrderId={}, currentStatus={}", platformOrderId, currentStatus);
            return false;
        }

        boolean success = dp.acceptOrder(platformOrderId);
        if (success) {
            // 修复 P1-5：接单也使用 CAS 更新，防止并发接单覆盖状态
            DeliveryOrder acceptEntity = new DeliveryOrder();
            acceptEntity.setStatus(DeliveryOrderStatus.ACCEPTED.getValue());
            acceptEntity.setUpdateTime(LocalDateTime.now());
            LambdaUpdateWrapper<DeliveryOrder> acceptWrapper = new LambdaUpdateWrapper<>();
            acceptWrapper.eq(DeliveryOrder::getId, order.getId())
                    .eq(DeliveryOrder::getStatus, currentStatus);
            int rows = deliveryOrderMapper.update(acceptEntity, acceptWrapper);
            if (rows == 0) {
                log.warn("接单失败：订单状态已被其他请求更新: id={}", order.getId());
                return false;
            }
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

        // 租户校验（fail-closed）
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null || !tenantId.equals(order.getTenantId())) {
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
        // 修复 P1-5：添加 CAS 条件（WHERE status = currentStatus），防止并发更新覆盖状态
        DeliveryOrder updateEntity = new DeliveryOrder();
        updateEntity.setStatus(status);
        updateEntity.setUpdateTime(order.getUpdateTime());
        LambdaUpdateWrapper<DeliveryOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, currentStatus);
        int rows = deliveryOrderMapper.update(updateEntity, updateWrapper);
        if (rows == 0) {
            log.warn("订单状态已被其他请求更新，更新失败: id={}, expectedStatus={}", id, currentStatus);
            return false;
        }
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
        // fail-closed：强制租户校验
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        if (tenantId == null) {
            stats.put("todayOrders", 0L);
            stats.put("pendingCount", 0L);
            stats.put("acceptedCount", 0L);
            stats.put("pickingCount", 0L);
            stats.put("deliveringCount", 0L);
            stats.put("completedCount", 0L);
            stats.put("cancelledCount", 0L);
            stats.put("totalCount", 0L);
            stats.put("totalAmount", BigDecimal.ZERO);
            return stats;
        }

        // 今日订单数（selectCount 不加载全量数据到内存，无 OOM 风险）
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

        // #71 使用聚合 SQL 替代全量 selectList + 内存遍历，防止 OOM
        String endDateParam = endDate != null ? LocalDate.parse(endDate).atTime(LocalTime.MAX).toString() : null;
        List<Map<String, Object>> aggRows = deliveryOrderMapper.selectStatsByStatus(tenantId, platform, startDate, endDateParam);

        long pendingCount = 0, acceptedCount = 0, pickingCount = 0;
        long deliveringCount = 0, completedCount = 0, cancelledCount = 0;
        long totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> row : aggRows) {
            String s = String.valueOf(row.get("status"));
            long cnt = row.get("cnt") == null ? 0 : ((Number) row.get("cnt")).longValue();
            BigDecimal total = row.get("total") == null ? BigDecimal.ZERO : new BigDecimal(row.get("total").toString());
            totalCount += cnt;
            totalAmount = totalAmount.add(total);

            if (DeliveryOrderStatus.PENDING.getValue().equals(s)) pendingCount = cnt;
            else if (DeliveryOrderStatus.ACCEPTED.getValue().equals(s)) acceptedCount = cnt;
            else if (DeliveryOrderStatus.PICKING.getValue().equals(s)) pickingCount = cnt;
            else if (DeliveryOrderStatus.DELIVERING.getValue().equals(s)) deliveringCount = cnt;
            else if (DeliveryOrderStatus.DELIVERED.getValue().equals(s)) completedCount = cnt;
            else if (DeliveryOrderStatus.CANCELLED.getValue().equals(s)) cancelledCount = cnt;
        }

        stats.put("pendingCount", pendingCount);
        stats.put("acceptedCount", acceptedCount);
        stats.put("pickingCount", pickingCount);
        stats.put("deliveringCount", deliveringCount);
        stats.put("completedCount", completedCount);
        stats.put("cancelledCount", cancelledCount);
        stats.put("totalCount", totalCount);
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

        // #8 签名校验：回调是外部请求无登录态，必须校验签名防伪造
        if (!dp.verifyCallback(params)) {
            log.warn("回调签名校验失败: platform={}", platform);
            throw new com.reggie.common.CustomException("回调签名校验失败");
        }

        String type = params.getOrDefault("type", "");
        String platformOrderId = params.getOrDefault("platformOrderId", "");
        String status = params.getOrDefault("status", "");

        log.info("收到[{}]平台回调: type={}, platformOrderId={}, status={}", platform, type, platformOrderId, status);

        // #9 回调无登录态，BaseContext.getTenantId() 恒为 null。
        // 改用跨租户查询：按 platform + platformOrderId 全局定位订单，再用订单的 tenantId 操作。
        DeliveryOrder order = deliveryOrderMapper.selectByPlatformOrderCrossTenant(platform, platformOrderId);

        // 为后续 updateById/insert 设置租户上下文（回调线程结束后在 finally 清理）
        Long callbackTenantId = null;
        if (order != null) {
            callbackTenantId = order.getTenantId();
        } else if ("new_order".equals(type)) {
            // 新订单回调：从回调参数获取 tenantId（平台账号映射到租户）
            String tenantIdStr = params.getOrDefault("tenantId", "");
            if (!tenantIdStr.isEmpty()) {
                try {
                    callbackTenantId = Long.valueOf(tenantIdStr);
                } catch (NumberFormatException e) {
                    log.warn("回调 tenantId 格式错误: {}", tenantIdStr);
                }
            }
        }

        try {
            if (callbackTenantId != null) {
                BaseContext.setCurrentTenantId(callbackTenantId);
            }

            switch (type) {
                case CALLBACK_TYPE_NEW_ORDER:
                    // 新订单通知：保存/更新订单
                    return handleNewOrderCallback(order, platform, params, callbackTenantId);

                case CALLBACK_TYPE_STATUS_UPDATE:
                    // 状态变更通知
                    if (order == null) {
                        log.warn("回调订单不存在: platform={}, platformOrderId={}", platform, platformOrderId);
                        return "error: order not found";
                    }
                    updateOrderStatusCallback(order, status);
                    return "success";

                case CALLBACK_TYPE_CANCEL:
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
        } finally {
            // 清理回调线程的租户上下文，防止线程复用导致串租户
            if (callbackTenantId != null) {
                BaseContext.setCurrentTenantId(null);
            }
        }
    }

    /**
     * 处理新订单回调：如果本地无记录则插入，已存在则跳过（幂等）
     * #9 tenantId 从回调参数获取，不依赖 BaseContext
     */
    private String handleNewOrderCallback(DeliveryOrder existOrder, String platform, Map<String, String> params, Long tenantId) {
        if (existOrder != null) {
            log.info("订单已存在（幂等跳过）: platformOrderId={}", params.get("platformOrderId"));
            return "success";
        }

        if (tenantId == null) {
            log.error("新订单回调缺少有效 tenantId: platformOrderId={}", params.get("platformOrderId"));
            return "error: missing tenantId";
        }

        DeliveryOrder newOrder = new DeliveryOrder();
        newOrder.setTenantId(tenantId);
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
        log.info("新订单入库: platform={}, platformOrderId={}, tenantId={}", platform, newOrder.getPlatformOrderId(), tenantId);
        return "success";
    }

    /**
     * 根据回调更新订单状态
     * #8 状态机校验：禁止从终态（DELIVERED/CANCELLED）回退，防止绕过状态机
     */
    private void updateOrderStatusCallback(DeliveryOrder order, String status) {
        String currentStatus = order.getStatus();
        // 终态不允许再流转（DELIVERED 和 CANCELLED）
        if (DeliveryOrderStatus.DELIVERED.getValue().equals(currentStatus)
                || DeliveryOrderStatus.CANCELLED.getValue().equals(currentStatus)) {
            log.warn("回调状态流转被拒绝（终态不可变更）: platformOrderId={}, currentStatus={}, targetStatus={}",
                    order.getPlatformOrderId(), currentStatus, status);
            return;
        }
        // 相同状态无需更新
        if (currentStatus != null && currentStatus.equals(status)) {
            return;
        }
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(order);
        log.info("回调更新订单状态: platformOrderId={} -> {}", order.getPlatformOrderId(), status);
    }
}
