package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.order.model.Orders;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static com.reggie.module.payment.model.PaymentOrder.STATUS_FAIL;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_PENDING;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;

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
        // 重复支付检查：同一订单已存在 SUCCESS 支付单则拒绝
        int successCount = baseMapper.countByOrderIdAndStatuses(orderId, "'" + STATUS_SUCCESS + "'");
        if (successCount > 0) {
            throw new CustomException("该订单已支付成功，请勿重复支付");
        }
        // 存在 PENDING 支付单则复用返回，避免重复创建支付单和重复调用渠道
        PaymentOrder existPending = lambdaQuery()
                .eq(PaymentOrder::getOrderId, orderId)
                .eq(PaymentOrder::getStatus, STATUS_PENDING)
                .one();
        if (existPending != null) {
            log.info("复用待支付订单: tradeNo={}, orderId={}", existPending.getTradeNo(), orderId);
            return existPending;
        }

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
    public PaymentOrder selectByTradeNoIgnoreTenant(String tradeNo) {
        return baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(String tradeNo, String channelTradeNo) {
        // 原子更新：仅当状态为 PENDING 时更新为 SUCCESS，解决回调 read-then-write 竞态（幂等）
        int affected = baseMapper.casUpdateStatus(tradeNo, STATUS_PENDING, STATUS_SUCCESS,
                channelTradeNo, LocalDateTime.now());
        if (affected == 0) {
            // 状态已非 PENDING（已成功/已失败/已退款），幂等跳过
            log.info("支付回调幂等跳过：支付单状态已非PENDING，tradeNo={}", tradeNo);
            return;
        }
        log.info("支付成功: tradeNo={}, channelTradeNo={}", tradeNo, channelTradeNo);

        // 联动更新业务订单状态：仅当订单为待付款(1)时才更新为待接单(2)，避免覆盖已取消/已完成订单（状态机校验）
        PaymentOrder po = baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
        if (po == null) {
            return;
        }
        // 回填租户上下文，确保后续业务订单查询走正确的租户隔离
        BaseContext.setCurrentTenantId(po.getTenantId());
        Orders order = orderService.getById(po.getOrderId());
        if (order != null && order.getStatus() != null) {
            if (Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
                order.setStatus(Orders.STATUS_ORDERED);
                order.setCheckoutTime(LocalDateTime.now());
                orderService.updateById(order);
                log.info("支付成功联动更新订单: orderId={}, orderStatus=待接单", po.getOrderId());
            } else {
                log.warn("支付成功但订单状态非待付款，跳过联动更新: orderId={}, currentStatus={}",
                        po.getOrderId(), order.getStatus());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFail(String tradeNo, String errorMsg) {
        // 原子更新：仅当状态为 PENDING 时更新为 FAIL（幂等）
        int affected = baseMapper.casUpdateStatus(tradeNo, STATUS_PENDING, STATUS_FAIL,
                null, LocalDateTime.now());
        if (affected == 0) {
            log.info("支付失败回调幂等跳过：支付单状态已非PENDING，tradeNo={}", tradeNo);
            return;
        }
        log.warn("支付失败: tradeNo={}, errorMsg={}", tradeNo, errorMsg);

        PaymentOrder po = baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
        if (po == null) {
            return;
        }
        // 回填租户上下文，确保后续业务订单查询走正确的租户隔离
        BaseContext.setCurrentTenantId(po.getTenantId());
        // 仅当订单为待付款时才联动取消，避免覆盖已配送/已完成订单（状态机校验）
        Orders order = orderService.getById(po.getOrderId());
        if (order != null && order.getStatus() != null
                && Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
            order.setStatus(Orders.STATUS_CANCELLED);
            orderService.updateById(order);
            log.warn("支付失败联动取消订单: orderId={}, reason={}", po.getOrderId(), errorMsg);
        } else if (order != null) {
            log.warn("支付失败但订单状态非待付款，跳过联动取消: orderId={}, currentStatus={}",
                    po.getOrderId(), order.getStatus());
        }
    }

    private String generateTradeNo() {
        // 使用UUID保证交易号唯一性，避免Math.random()的并发问题
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}


