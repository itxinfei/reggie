package com.reggie.module.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Real-time Message Service (using in-memory store for polling)
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@Service
public class WebSocketMessageService {

    // In-memory notification store for polling
    private final Map<Long, List<Map<String, Object>>> tenantNotifications = new ConcurrentHashMap<>();
    private static final int MAX_NOTIFICATIONS = 100;

    /**
     * Send order notification
     */
    public void sendOrderNotification(Long orderId, String orderNumber, String type, String message) {
        Map<String, Object> notification = new ConcurrentHashMap<>();
        notification.put("orderId", orderId);
        notification.put("orderNumber", orderNumber);
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("read", false);

        // Store for polling
        Long tenantId = 1L; // Default tenant
        tenantNotifications.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(notification);
        
        // Keep only recent notifications
        List<Map<String, Object>> notifications = tenantNotifications.get(tenantId);
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        log.info("Order notification stored: orderId={}, type={}", orderId, type);
    }

    /**
     * Send kitchen notification
     */
    public void sendKitchenNotification(Long orderId, String orderNumber, String items, String type) {
        Map<String, Object> notification = new ConcurrentHashMap<>();
        notification.put("orderId", orderId);
        notification.put("orderNumber", orderNumber);
        notification.put("items", items);
        notification.put("type", type);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("category", "kitchen");

        Long tenantId = 1L;
        tenantNotifications.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(notification);
        log.info("Kitchen notification stored: orderId={}, type={}", orderId, type);
    }

    /**
     * Send system notification
     */
    public void sendSystemNotification(String type, String title, String message) {
        Map<String, Object> notification = new ConcurrentHashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("category", "system");

        Long tenantId = 1L;
        tenantNotifications.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(notification);
        log.info("System notification stored: type={}, title={}", type, title);
    }

    /**
     * Get notifications for tenant (for polling)
     */
    public List<Map<String, Object>> getNotifications(Long tenantId) {
        return tenantNotifications.getOrDefault(tenantId, new CopyOnWriteArrayList<>());
    }

    /**
     * Get unread notification count
     */
    public int getUnreadCount(Long tenantId) {
        List<Map<String, Object>> notifications = tenantNotifications.getOrDefault(tenantId, new CopyOnWriteArrayList<>());
        return (int) notifications.stream().filter(n -> Boolean.FALSE.equals(n.get("read"))).count();
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long tenantId, int index) {
        List<Map<String, Object>> notifications = tenantNotifications.getOrDefault(tenantId, new CopyOnWriteArrayList<>());
        if (index >= 0 && index < notifications.size()) {
            notifications.get(index).put("read", true);
        }
    }

    /**
     * Clear all notifications
     */
    public void clearNotifications(Long tenantId) {
        tenantNotifications.remove(tenantId);
    }
}

