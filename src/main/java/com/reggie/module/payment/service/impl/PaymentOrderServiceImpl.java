package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
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

import static com.reggie.module.payment.model.PaymentOrder.*;

@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

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
        if (po != null) {
            // 幂等保护：已成功的订单不再重复处理
            if (STATUS_SUCCESS.equals(po.getStatus())) {
                log.info("支付回调幂等：订单已处理过，跳过 tradeNo={}", tradeNo);
                return;
            }
            po.setStatus(STATUS_SUCCESS);
            po.setChannelTradeNo(channelTradeNo);
            po.setPaidTime(LocalDateTime.now());
            updateById(po);

            // 联动更新业务订单状态：待接单(2) → 已完成(4)
            Orders order = orderService.getById(po.getOrderId());
            if (order != null && order.getStatus() != null) {
                order.setStatus(Orders.STATUS_COMPLETED);
                order.setCheckoutTime(LocalDateTime.now());
                orderService.updateById(order);
                log.info("支付成功联动更新订单: orderId={}, orderStatus=已完成", po.getOrderId());
            }

            log.info("支付成功: tradeNo={}, channelTradeNo={}", tradeNo, channelTradeNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFail(String tradeNo, String errorMsg) {
        PaymentOrder po = lambdaQuery().eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po != null) {
            po.setStatus(STATUS_FAIL);
            updateById(po);

            // 联动更新业务订单状态为已取消
            Orders order = orderService.getById(po.getOrderId());
            if (order != null && order.getStatus() != null) {
                order.setStatus(Orders.STATUS_CANCELLED);
                orderService.updateById(order);
                log.warn("支付失败联动取消订单: orderId={}, reason={}", po.getOrderId(), errorMsg);
            }

            log.info("支付失败: tradeNo={}, errorMsg={}", tradeNo, errorMsg);
        }
    }

    private String generateTradeNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%04d", (int)(Math.random() * 10000));
    }
}
