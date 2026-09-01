package com.reggie.module.notification.controller;

import com.reggie.common.R;
import com.reggie.module.notification.service.WebSocketMessageService;
import com.reggie.module.notification.service.VoiceBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * User Experience Controller
 * 
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/ux")
@Tag(name = "用户体验管理")
public class UserExperienceController {

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    @Autowired
    private VoiceBroadcastService voiceBroadcastService;

    // ==================== WebSocket Management ====================

    @PostMapping("/notification/order")
    @Operation(summary = "发送下单通知")
    public R<String> sendOrderNotification(
                        @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "通知类型") @RequestParam String type,
            @Parameter(description = "消息内容") @RequestParam String message) {
        webSocketMessageService.sendOrderNotification(orderId, orderNumber, type, message);
        return R.success("Notification sent");
    }

    @PostMapping("/notification/kitchen")
    @Operation(summary = "发送后厨通知")
    public R<String> sendKitchenNotification(
                        @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "菜品明细摘要") @RequestParam String items,
            @Parameter(description = "类型") @RequestParam(defaultValue = "new") String type) {
        webSocketMessageService.sendKitchenNotification(orderId, orderNumber, items, type);
        return R.success("Kitchen notification sent");
    }

    @PostMapping("/notification/system")
    @Operation(summary = "发送系统通知")
    public R<String> sendSystemNotification(
                        @Parameter(description = "类型") @RequestParam String type,
            @Parameter(description = "标题") @RequestParam String title,
            @Parameter(description = "消息内容") @RequestParam String message) {
        webSocketMessageService.sendSystemNotification(type, title, message);
        return R.success("System notification sent");
    }

    // ==================== Voice Broadcast ====================

    @GetMapping("/voice/new-order")
    @Operation(summary = "新订单语音播报")
    public R<Map<String, Object>> getNewOrderVoice(
                        @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "金额") @RequestParam String amount) {
        Map<String, Object> voiceData = voiceBroadcastService.generateNewOrderVoice(orderNumber, amount);
        return R.success(voiceData);
    }

    @GetMapping("/voice/order-reminder")
    @Operation(summary = "催单语音播报")
    public R<Map<String, Object>> getOrderReminderVoice(
                        @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "等待分钟数") @RequestParam int minutes) {
        Map<String, Object> voiceData = voiceBroadcastService.generateOrderReminderVoice(orderNumber, minutes);
        return R.success(voiceData);
    }

    @GetMapping("/voice/payment-received")
    @Operation(summary = "收款到账语音播报")
    public R<Map<String, Object>> getPaymentReceivedVoice(
                        @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "金额") @RequestParam String amount) {
        Map<String, Object> voiceData = voiceBroadcastService.generatePaymentReceivedVoice(orderNumber, amount);
        return R.success(voiceData);
    }

    @GetMapping("/voice/queue-call")
    @Operation(summary = "叫号语音播报")
    public R<Map<String, Object>> getQueueCallVoice(
                        @Parameter(description = "排队号") @RequestParam String queueNumber,
            @Parameter(description = "桌名") @RequestParam String tableName) {
        Map<String, Object> voiceData = voiceBroadcastService.generateQueueCallVoice(queueNumber, tableName);
        return R.success(voiceData);
    }
}


