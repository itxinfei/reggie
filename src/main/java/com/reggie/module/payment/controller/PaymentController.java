package com.reggie.module.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.PayRequestDTO;
import com.reggie.dto.RefundRequestDTO;
import com.reggie.entity.Orders;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.channel.PayRequest;
import com.reggie.module.payment.channel.PayResponse;
import com.reggie.module.payment.channel.RefundRequest;
import com.reggie.module.payment.channel.RefundResponse;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_REFUND;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import com.reggie.service.DashboardService;
import com.reggie.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 聚合支付控制器
 * 提供统一支付、退款、回调处理等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
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
    private OrderService orderService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @Autowired
    private DashboardService dashboardService;

    @PostMapping("/pay")
    @Operation(summary = "创建支付", description = "创建支付订单并调用支付渠道生成支付链接/二维码")
    public R<PayResponse> pay(@Validated @RequestBody PayRequestDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.createPaymentOrder(dto.getOrderId(), dto.getChannel(), dto.getAmount());

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(dto.getChannel());
        PayRequest request = new PayRequest();
        request.setTradeNo(paymentOrder.getTradeNo());
        request.setAmount(dto.getAmount());
        request.setSubject("瑞吉外卖-订单" + dto.getOrderId());
        PayResponse response = paymentChannel.createOrder(request);

        return R.success(response);
    }

    @PostMapping("/notify/{channel}")
    @Operation(summary = "支付回调", description = "接收支付渠道的异步通知，更新订单支付状态")
    @Parameter(name = "channel", description = "支付渠道：WECHAT-微信、ALIPAY-支付宝", required = true)
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
    @Operation(summary = "退款", description = "申请退款并调用支付渠道处理退款")
    public R<String> refund(@Validated @RequestBody RefundRequestDTO dto) {
        PaymentOrder paymentOrder = paymentOrderService.getById(dto.getPaymentOrderId());
        if (paymentOrder == null) {
            return R.error("支付订单不存在");
        }

        // 金额校验：退款金额不能大于支付订单金额
        if (dto.getAmount() != null && dto.getAmount().compareTo(paymentOrder.getAmount()) > 0) {
            return R.error("退款金额不能大于支付金额（支付金额：" + paymentOrder.getAmount() + "元）");
        }

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(paymentOrder.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        refundRequest.setAmount(dto.getAmount());
        refundRequest.setReason(dto.getReason());
        RefundResponse refundResponse = paymentChannel.refund(refundRequest);

        if (refundResponse.isSuccess()) {
            RefundRecord record = new RefundRecord();
            record.setPaymentOrderId(dto.getPaymentOrderId());
            record.setRefundNo(generateRefundNo());
            record.setAmount(dto.getAmount());
            record.setReason(dto.getReason());
            record.setStatus(STATUS_SUCCESS);
            refundRecordService.save(record);

            paymentOrder.setStatus(STATUS_REFUND);
            paymentOrderService.updateById(paymentOrder);

            // 联动更新业务订单状态为已退款(6)
            Orders order = orderService.getById(paymentOrder.getOrderId());
            if (order != null) {
                order.setStatus(Orders.STATUS_REFUNDED);
                orderService.updateById(order);
                log.info("退款成功联动更新订单: orderId={}, orderStatus=已退款", paymentOrder.getOrderId());
            }

            // 修改点：退款成功后清除 Dashboard 缓存，确保今日订单/营业额数据实时准确
            clearDashboardCache();
            return R.success("退款成功");
        }
        return R.error("退款失败: " + refundResponse.getErrorMsg());
    }

    @GetMapping("/query/{tradeNo}")
    @Operation(summary = "查询支付状态", description = "根据交易号查询支付订单状态")
    @Parameter(name = "tradeNo", description = "交易号", required = true)
    public R<PaymentOrder> query(@PathVariable String tradeNo) {
        PaymentOrder po = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            return R.error("支付订单不存在");
        }
        return R.success(po);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询支付订单列表，支持按订单ID、渠道、状态、时间范围筛选")
    public R<Page<PaymentOrder>> page(int page, int pageSize,
            @Parameter(description = "订单ID") Long orderId,
            @Parameter(description = "支付渠道：ALIPAY-支付宝, WECHAT-微信") String channel,
            @Parameter(description = "支付状态：PENDING-待支付, SUCCESS-成功, FAIL/FAILED-失败, REFUND/REFUNDED-已退款") String status,
            @Parameter(description = "开始时间（yyyy-MM-dd HH:mm:ss）") String beginTime,
            @Parameter(description = "结束时间（yyyy-MM-dd HH:mm:ss）") String endTime) {
        Page<PaymentOrder> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PaymentOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PaymentOrder::getOrderId, orderId);
        // 修改点：支持按支付渠道筛选
        qw.eq(channel != null && !channel.isEmpty(), PaymentOrder::getChannel, channel);
        // 修改点：支持按支付状态筛选，兼容前端传 FAILED 或 FAIL
        if (status != null && !status.isEmpty()) {
            if ("FAILED".equals(status)) {
                qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_FAIL);
            } else if ("REFUNDED".equals(status)) {
                qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_REFUND);
            } else {
                qw.eq(PaymentOrder::getStatus, status);
            }
        }
        // 修改点：支持按时间范围筛选（基于创建时间）
        if (beginTime != null && !beginTime.isEmpty()) {
            qw.ge(PaymentOrder::getCreatedTime, java.time.LocalDateTime.parse(beginTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            qw.le(PaymentOrder::getCreatedTime, java.time.LocalDateTime.parse(endTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        qw.orderByDesc(PaymentOrder::getCreatedTime);
        paymentOrderService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%04d", (int)(Math.random() * 10000));
    }

    /**
     * 清除 Dashboard 缓存（退款后调用，确保概览数据实时准确）
     */
    private void clearDashboardCache() {
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            if (dashboardService != null && tenantId != null) {
                dashboardService.clearOverviewCache(tenantId);
            }
        } catch (Exception e) {
            log.warn("清除Dashboard缓存失败: {}", e.getMessage());
        }
    }
}

