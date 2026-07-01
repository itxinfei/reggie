package com.reggie.module.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.payment.channel.*;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_REFUND;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@Tag(name = "聚合支付")
public class PaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @PostMapping("/pay")
    @Operation(summary = "创建支付")
    public R<PayResponse> pay(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        String channel = (String) params.get("channel");
        BigDecimal amount = new BigDecimal(params.get("amount").toString());

        PaymentOrder paymentOrder = paymentOrderService.createPaymentOrder(orderId, channel, amount);

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(channel);
        PayRequest request = new PayRequest();
        request.setTradeNo(paymentOrder.getTradeNo());
        request.setAmount(amount);
        request.setSubject("瑞吉外卖-订单" + orderId);
        PayResponse response = paymentChannel.createOrder(request);

        return R.success(response);
    }

    @PostMapping("/notify/{channel}")
    @Operation(summary = "支付回调")
    public R<String> notify(@PathVariable String channel, @RequestBody Map<String, String> params) {
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(channel);
        PayResponse response = paymentChannel.handleNotify(params);
        if (response.isSuccess()) {
            String tradeNo = params.get("out_trade_no");
            if (tradeNo != null) {
                paymentOrderService.handlePaymentSuccess(tradeNo, response.getChannelTradeNo());
            }
            return R.success("回调处理成功");
        }
        return R.error("回调处理失败");
    }

    @PostMapping("/refund")
    @Operation(summary = "退款")
    public R<String> refund(@RequestBody Map<String, Object> params) {
        Long paymentOrderId = Long.valueOf(params.get("paymentOrderId").toString());
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String reason = (String) params.get("reason");

        PaymentOrder paymentOrder = paymentOrderService.getById(paymentOrderId);
        if (paymentOrder == null) {
            return R.error("支付订单不存在");
        }

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(paymentOrder.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        refundRequest.setAmount(amount);
        refundRequest.setReason(reason);
        RefundResponse refundResponse = paymentChannel.refund(refundRequest);

        if (refundResponse.isSuccess()) {
            RefundRecord record = new RefundRecord();
            record.setPaymentOrderId(paymentOrderId);
            record.setRefundNo(generateRefundNo());
            record.setAmount(amount);
            record.setReason(reason);
            record.setStatus(STATUS_SUCCESS);
            refundRecordService.save(record);

            paymentOrder.setStatus(STATUS_REFUND);
            paymentOrderService.updateById(paymentOrder);

            return R.success("退款成功");
        }
        return R.error("退款失败: " + refundResponse.getErrorMsg());
    }

    @GetMapping("/query/{tradeNo}")
    @Operation(summary = "查询支付状态")
    public R<PaymentOrder> query(@PathVariable String tradeNo) {
        PaymentOrder po = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            return R.error("支付订单不存在");
        }
        return R.success(po);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<PaymentOrder>> page(int page, int pageSize, Long orderId) {
        Page<PaymentOrder> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PaymentOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PaymentOrder::getOrderId, orderId);
        qw.orderByDesc(PaymentOrder::getCreatedTime);
        paymentOrderService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%04d", (int)(Math.random() * 10000));
    }
}
