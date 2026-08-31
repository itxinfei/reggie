package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.service.PlatformOrderPersistService;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台订单落库服务实现
 * <p>
 * 将平台订单幂等写入本地 orders / order_detail。平台原始订单号（platform_order_id）
 * 用于去重：同一平台订单只落库一次，避免定时任务重复拉取导致重复订单。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Service
public class PlatformOrderPersistServiceImpl implements PlatformOrderPersistService {

    /** 平台订单默认状态：待接单（与顾客端下单态对齐，由商家在后台接单） */
    private static final int PLATFORM_ORDER_STATUS = Orders.STATUS_ORDERED;

    /** 平台外卖默认支付方式：微信（仅占位，以平台实际支付为准） */
    private static final int PLATFORM_PAY_METHOD = 2;

    /** 平台订单落库后是否自动打印（外卖单 DELIVERY + 后厨单 KITCHEN，终端按 print_types 过滤） */
    private static final boolean PLATFORM_AUTO_PRINT = true;

    private final OrderMapper orderMapper;
    private final OrderDetailService orderDetailService;
    private final PrinterService printerService;

    @Autowired
    public PlatformOrderPersistServiceImpl(OrderMapper orderMapper, OrderDetailService orderDetailService,
                                           PrinterService printerService) {
        this.orderMapper = orderMapper;
        this.orderDetailService = orderDetailService;
        this.printerService = printerService;
    }

    @Override
    public boolean exists(String platformType, String platformOrderId, Long tenantId) {
        if (StringUtils.isBlank(platformOrderId)) {
            return false;
        }
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getPlatformType, platformType)
                .eq(Orders::getPlatformOrderId, platformOrderId)
                .eq(Orders::getTenantId, tenantId));
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int persistOrders(String platformType, String platformShopId, Long tenantId,
                             List<PlatformOrder> platformOrders) {
        if (platformOrders == null || platformOrders.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        for (PlatformOrder po : platformOrders) {
            if (po == null || StringUtils.isBlank(po.getPlatformOrderId())) {
                log.warn("[平台落库] 跳过无效订单(platformOrderId 为空): platformType={}", platformType);
                continue;
            }
            if (exists(platformType, po.getPlatformOrderId(), tenantId)) {
                log.info("[平台落库] 订单已存在，跳过去重: platformOrderId={}", po.getPlatformOrderId());
                continue;
            }
            Orders order = toLocalOrder(po, platformType, platformShopId, tenantId);
            orderMapper.insert(order);
            List<OrderDetail> details = toOrderDetails(po, order.getId());
            if (!details.isEmpty()) {
                orderDetailService.saveBatch(details);
            }
            inserted++;
            log.info("[平台落库] 新增平台订单: platformType={}, platformOrderId={}, localId={}, amount={}",
                    platformType, po.getPlatformOrderId(), order.getId(), order.getAmount());
            // 落库后自动打印（外卖单 + 后厨单），由门店 PC 打印代理静默出票；
            // 打印异常隔离，不影响平台订单落库事务
            autoPrint(order);
        }
        return inserted;
    }

    /**
     * 平台订单自动打印
     *
     * <p>向订单所属租户的启用终端派发 DELIVERY（外卖单，含顾客/平台信息）与 KITCHEN
     * （后厨制作单）两类打印任务。终端 print_types 为空视为接收全部类型，否则按
     * 逗号分隔精确匹配——门店可将打印机配置为只收某一类，天然过滤重复打单。</p>
     *
     * @param order 已落库的平台订单
     */
    private void autoPrint(Orders order) {
        if (!PLATFORM_AUTO_PRINT) {
            return;
        }
        try {
            printerService.printOrder(order.getId(), "DELIVERY");
            printerService.printOrder(order.getId(), "KITCHEN");
        } catch (Exception e) {
            log.error("[平台落库] 平台订单自动打印失败, orderId={}, platformOrderId={}",
                    order.getId(), order.getPlatformOrderId(), e);
        }
    }

    /**
     * 将平台标准化订单映射为本地 Orders 实体
     */
    private Orders toLocalOrder(PlatformOrder po, String platformType, String platformShopId, Long tenantId) {
        Orders order = new Orders();
        order.setId(IdWorker.getId());
        order.setNumber(generateOrderNumber());
        order.setStatus(PLATFORM_ORDER_STATUS);
        order.setAmount(po.getAmount() != null ? po.getAmount() : BigDecimal.ZERO);
        // 平台订单无本地用户，userId 置 0 以通过字段非空校验和数据库约束
        order.setUserId(0L);
        order.setPayMethod(PLATFORM_PAY_METHOD);
        order.setRemark(StringUtils.isNotBlank(po.getRemark()) ? po.getRemark() : "平台外卖订单");
        order.setSource("TAKEOUT");
        order.setUserName(StringUtils.isNotBlank(po.getCustomerName()) ? po.getCustomerName() : "平台顾客");
        order.setPhone(StringUtils.defaultString(po.getCustomerPhone()));
        order.setAddress(StringUtils.defaultString(po.getAddress()));
        order.setConsignee(StringUtils.defaultString(po.getCustomerName()));
        order.setOrderTime(parseOrderTime(po.getOrderTime()));
        order.setPlatformType(platformType);
        order.setPlatformOrderId(po.getPlatformOrderId());
        order.setPlatformShopId(platformShopId);
        order.setPlatformRaw(po.getRawJson());
        // platformRaw 字段承载原始 JSON，便于后续字段补全；原 PlatformOrder 无该字段时置空
        if (order.getPlatformRaw() == null) {
            order.setPlatformRaw("");
        }
        order.setTenantId(tenantId);
        return order;
    }

    /**
     * 将平台订单明细映射为本地 OrderDetail 列表
     */
    private List<OrderDetail> toOrderDetails(PlatformOrder po, Long orderId) {
        List<OrderDetail> details = new ArrayList<>();
        if (po.getItems() == null) {
            return details;
        }
        for (PlatformOrder.OrderItem item : po.getItems()) {
            OrderDetail d = new OrderDetail();
            d.setName(StringUtils.defaultString(item.getDishName(), "未知菜品"));
            d.setDishId(null);
            d.setSetmealId(null);
            d.setDishFlavor(StringUtils.defaultString(item.getFlavor()));
            d.setNumber(item.getQuantity() != null ? item.getQuantity() : 1);
            d.setAmount(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
            d.setOrderId(orderId);
            d.setTenantId(null);
            details.add(d);
        }
        return details;
    }

    /**
     * 生成订单号（日期 + 雪花后缀，兼容现有 number 字段长度）
     */
    private String generateOrderNumber() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.valueOf(System.nanoTime() % 100000);
    }

    /**
     * 解析平台下单时间，失败回退当前时间
     */
    private LocalDateTime parseOrderTime(String orderTime) {
        if (StringUtils.isBlank(orderTime)) {
            return LocalDateTime.now();
        }
        try {
            // 平台订单时间多为 ISO 8601：yyyy-MM-dd HH:mm:ss 或带 T
            String normalized = orderTime.replace('T', ' ');
            if (normalized.contains(".")) {
                normalized = normalized.substring(0, normalized.indexOf('.'));
            }
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.warn("[平台落库] 订单时间解析失败，使用当前时间: orderTime={}", orderTime);
            return LocalDateTime.now();
        }
    }
}
