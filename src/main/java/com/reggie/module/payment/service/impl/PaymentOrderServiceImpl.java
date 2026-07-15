package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.entity.Orders;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static com.reggie.module.payment.model.PaymentOrder.*;

/**
 * 支付订单服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPaymentOrder(Long orderId, String channel, BigDecimal amount) {
        PaymentOrder po = new PaymentOrder();
        po.setOrderId(orderId);
        po.setTenantId(BaseContext.getCurrentTenantId());
        po.setTradeNo(generateTradeNo());
        po.setChannel(channel);
        po.setAmount(amount);
        po.setStatus(STATUS_PENDING);
        save(po);
        log.info("创建支付订单: tradeNo={}, orderId={}, channel={}, amount={}", po.getTradeNo(), orderId, channel, amount);
        return po;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(String tradeNo, String channelTradeNo) {
        PaymentOrder po = lambdaQuery().eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            log.warn("支付回调处理失败：支付订单不存在，tradeNo={}", tradeNo);
            throw new CustomException("支付订单不存在，tradeNo=" + tradeNo);
        }
        // 幂等保护：已成功的订单不再重复处理
        if (STATUS_SUCCESS.equals(po.getStatus())) {
            log.info("支付回调幂等：订单已处理过，跳过 tradeNo={}", tradeNo);
            return;
        }
        po.setStatus(STATUS_SUCCESS);
        po.setChannelTradeNo(channelTradeNo);
        po.setPaidTime(LocalDateTime.now());
        updateById(po);

        // 联动更新业务订单状态：待付款(1) → 待接单(2)
        Orders order = orderService.getById(po.getOrderId());
        if (order != null && order.getStatus() != null) {
            if (!Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
                log.warn("支付成功联动更新异常：订单状态不是待付款，orderId={}, currentStatus={}",
                        po.getOrderId(), order.getStatus());
            }
            order.setStatus(Orders.STATUS_ORDERED);
            order.setCheckoutTime(LocalDateTime.now());
            orderService.updateById(order);
            log.info("支付成功联动更新订单: orderId={}, orderStatus=待接单", po.getOrderId());
        }

        log.info("支付成功: tradeNo={}, channelTradeNo={}", tradeNo, channelTradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFail(String tradeNo, String errorMsg) {
        PaymentOrder po = lambdaQuery().eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            log.warn("支付失败处理失败：支付订单不存在，tradeNo={}", tradeNo);
            throw new CustomException("支付订单不存在，tradeNo=" + tradeNo);
        }
        po.setStatus(STATUS_FAIL);
        updateById(po);

        // 联动更新业务订单状态为已取消
        Orders order = orderService.getById(po.getOrderId());
        if (order != null && order.getStatus() != null) {
            if (!Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
                log.warn("支付失败联动取消异常：订单状态不是待付款，orderId={}, currentStatus={}",
                        po.getOrderId(), order.getStatus());
            }
            order.setStatus(Orders.STATUS_CANCELLED);
            orderService.updateById(order);
            log.warn("支付失败联动取消订单: orderId={}, reason={}", po.getOrderId(), errorMsg);
        }

        log.warn("支付失败: tradeNo={}, errorMsg={}", tradeNo, errorMsg);
    }

    private String generateTradeNo() {
        // 使用UUID保证交易号唯一性，避免Math.random()的并发问题
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
