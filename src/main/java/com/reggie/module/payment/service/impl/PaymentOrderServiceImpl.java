package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_FAIL;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_PENDING;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

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
            po.setStatus(STATUS_SUCCESS);
            po.setChannelTradeNo(channelTradeNo);
            po.setPaidTime(LocalDateTime.now());
            updateById(po);
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
            log.info("支付失败: tradeNo={}, errorMsg={}", tradeNo, errorMsg);
        }
    }

    private String generateTradeNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%04d", (int)(Math.random() * 10000));
    }
}
