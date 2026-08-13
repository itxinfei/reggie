package com.reggie.module.notification.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Voice Broadcast Service
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class VoiceBroadcastService {

    /**
     * Generate voice broadcast data for new order
     *
     * @param orderNumber Order number
     * @param amount      Order amount
     * @return Voice broadcast data
     */
    public Map<String, Object> generateNewOrderVoice(String orderNumber, String amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "new_order");
        data.put("text", "您有新订单，订单号" + formatOrderNumber(orderNumber) + "，金额" + amount + "元");
        data.put("orderNumber", orderNumber);
        data.put("amount", amount);
        data.put("priority", "high");
        return data;
    }

    /**
     * Generate voice broadcast data for order reminder
     *
     * @param orderNumber Order number
     * @param minutes     Minutes since order
     * @return Voice broadcast data
     */
    public Map<String, Object> generateOrderReminderVoice(String orderNumber, int minutes) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "order_reminder");
        data.put("text", "订单" + formatOrderNumber(orderNumber) + "已等待" + minutes + "分钟，请尽快处理");
        data.put("orderNumber", orderNumber);
        data.put("minutes", minutes);
        data.put("priority", minutes > 10 ? "high" : "normal");
        return data;
    }

    /**
     * Generate voice broadcast data for delivery arrival
     *
     * @param orderNumber Order number
     * @return Voice broadcast data
     */
    public Map<String, Object> generateDeliveryArrivalVoice(String orderNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "delivery_arrival");
        data.put("text", "骑手已到达，订单号" + formatOrderNumber(orderNumber));
        data.put("orderNumber", orderNumber);
        data.put("priority", "normal");
        return data;
    }

    /**
     * Generate voice broadcast data for payment received
     *
     * @param orderNumber Order number
     * @param amount      Payment amount
     * @return Voice broadcast data
     */
    public Map<String, Object> generatePaymentReceivedVoice(String orderNumber, String amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "payment_received");
        data.put("text", "收款成功，订单" + formatOrderNumber(orderNumber) + "，金额" + amount + "元");
        data.put("orderNumber", orderNumber);
        data.put("amount", amount);
        data.put("priority", "normal");
        return data;
    }

    /**
     * Generate voice broadcast data for queue number
     *
     * @param queueNumber Queue number
     * @param tableName   Table name
     * @return Voice broadcast data
     */
    public Map<String, Object> generateQueueCallVoice(String queueNumber, String tableName) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "queue_call");
        data.put("text", "请" + queueNumber + "号顾客到" + tableName + "就餐");
        data.put("queueNumber", queueNumber);
        data.put("tableName", tableName);
        data.put("priority", "high");
        return data;
    }

    /**
     * Format order number for voice broadcast
     * Remove long numbers and make it easier to read
     */
    private String formatOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.length() <= 6) {
            return orderNumber;
        }
        // Return last 6 digits for easier reading
        return orderNumber.substring(orderNumber.length() - 6);
    }
}
