package com.reggie.module.notification.controller;

import com.reggie.common.R;
import com.reggie.module.notification.service.WebSocketMessageService;
import com.reggie.module.notification.service.VoiceBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/ux")
@Tag(name = "User Experience Management")
public class UserExperienceController {

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    @Autowired
    private VoiceBroadcastService voiceBroadcastService;

    // ==================== WebSocket Management ====================

    @PostMapping("/notification/order")
    @Operation(summary = "Send order notification")
    public R<String> sendOrderNotification(
            @Parameter(description = "O r d e r I d")
            @Parameter(description = "Order ID") @RequestParam Long orderId,
            @Parameter(description = "Order number") @RequestParam String orderNumber,
            @Parameter(description = "Notification type") @RequestParam String type,
            @Parameter(description = "Message") @RequestParam String message) {
        webSocketMessageService.sendOrderNotification(orderId, orderNumber, type, message);
        return R.success("Notification sent");
    }

    @PostMapping("/notification/kitchen")
    @Operation(summary = "Send kitchen notification")
    public R<String> sendKitchenNotification(
            @Parameter(description = "O r d e r I d")
            @Parameter(description = "Order ID") @RequestParam Long orderId,
            @Parameter(description = "Order number") @RequestParam String orderNumber,
            @Parameter(description = "Items summary") @RequestParam String items,
            @Parameter(description = "Type") @RequestParam(defaultValue = "new") String type) {
        webSocketMessageService.sendKitchenNotification(orderId, orderNumber, items, type);
        return R.success("Kitchen notification sent");
    }

    @PostMapping("/notification/system")
    @Operation(summary = "Send system notification")
    public R<String> sendSystemNotification(
            @Parameter(description = "T y p e")
            @Parameter(description = "Type") @RequestParam String type,
            @Parameter(description = "Title") @RequestParam String title,
            @Parameter(description = "Message") @RequestParam String message) {
        webSocketMessageService.sendSystemNotification(type, title, message);
        return R.success("System notification sent");
    }

    // ==================== Voice Broadcast ====================

    @GetMapping("/voice/new-order")
    @Operation(summary = "Get new order voice data")
    public R<Map<String, Object>> getNewOrderVoice(
            @Parameter(description = "O r d e r N u m b e r")
            @Parameter(description = "Order number") @RequestParam String orderNumber,
            @Parameter(description = "Amount") @RequestParam String amount) {
        Map<String, Object> voiceData = voiceBroadcastService.generateNewOrderVoice(orderNumber, amount);
        return R.success(voiceData);
    }

    @GetMapping("/voice/order-reminder")
    @Operation(summary = "Get order reminder voice data")
    public R<Map<String, Object>> getOrderReminderVoice(
            @Parameter(description = "O r d e r N u m b e r")
            @Parameter(description = "Order number") @RequestParam String orderNumber,
            @Parameter(description = "Minutes") @RequestParam int minutes) {
        Map<String, Object> voiceData = voiceBroadcastService.generateOrderReminderVoice(orderNumber, minutes);
        return R.success(voiceData);
    }

    @GetMapping("/voice/payment-received")
    @Operation(summary = "Get payment received voice data")
    public R<Map<String, Object>> getPaymentReceivedVoice(
            @Parameter(description = "O r d e r N u m b e r")
            @Parameter(description = "Order number") @RequestParam String orderNumber,
            @Parameter(description = "Amount") @RequestParam String amount) {
        Map<String, Object> voiceData = voiceBroadcastService.generatePaymentReceivedVoice(orderNumber, amount);
        return R.success(voiceData);
    }

    @GetMapping("/voice/queue-call")
    @Operation(summary = "Get queue call voice data")
    public R<Map<String, Object>> getQueueCallVoice(
            @Parameter(description = "Q u e u e N u m b e r")
            @Parameter(description = "Queue number") @RequestParam String queueNumber,
            @Parameter(description = "Table name") @RequestParam String tableName) {
        Map<String, Object> voiceData = voiceBroadcastService.generateQueueCallVoice(queueNumber, tableName);
        return R.success(voiceData);
    }
}


