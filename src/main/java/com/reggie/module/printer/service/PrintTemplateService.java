package com.reggie.module.printer.service;

import com.reggie.module.order.model.Orders;
import com.reggie.module.order.model.OrderDetail;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Print Template Service
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class PrintTemplateService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate order receipt content
     *
     * @param order   Order information
     * @param details Order details
     * @return Receipt content
     */
    public Map<String, Object> generateOrderReceipt(Orders order, List<OrderDetail> details) {
        Map<String, Object> receipt = new HashMap<>();

        // Header
        receipt.put("title", "订单小票");
        receipt.put("storeName", "瑞吉外卖");
        receipt.put("orderNumber", order.getNumber());
        receipt.put("orderTime", order.getOrderTime() != null ? order.getOrderTime().format(DATE_FORMAT) : "");

        // Items
        StringBuilder items = new StringBuilder();
        items.append("--------------------------------\n");
        items.append(String.format("%-12s %4s %8s\n", "商品", "数量", "金额"));
        items.append("--------------------------------\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDetail detail : details) {
            items.append(String.format("%-12s %4d %8.2f\n",
                    truncateString(detail.getName(), 12),
                    detail.getNumber(),
                    detail.getAmount().multiply(new BigDecimal(detail.getNumber()))));
            totalAmount = totalAmount.add(detail.getAmount().multiply(new BigDecimal(detail.getNumber())));
        }

        items.append("--------------------------------\n");
        receipt.put("items", items.toString());

        // Summary
        receipt.put("totalAmount", totalAmount);
        receipt.put("totalAmountText", "合计：¥" + totalAmount.setScale(2));

        // Customer info
        if (order.getUserName() != null) {
            receipt.put("customerName", order.getUserName());
        }
        if (order.getPhone() != null) {
            receipt.put("customerPhone", order.getPhone());
        }
        if (order.getAddress() != null) {
            receipt.put("deliveryAddress", order.getAddress());
        }

        // Remark
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            receipt.put("remark", "备注：" + order.getRemark());
        }

        // Footer
        receipt.put("footer", "感谢您的光临！");
        receipt.put("printTime", java.time.LocalDateTime.now().format(DATE_FORMAT));

        return receipt;
    }

    /**
     * Generate kitchen order content
     *
     * @param order   Order information
     * @param details Order details
     * @return Kitchen order content
     */
    public Map<String, Object> generateKitchenOrder(Orders order, List<OrderDetail> details) {
        Map<String, Object> kitchenOrder = new HashMap<>();

        kitchenOrder.put("title", "厨房出单");
        kitchenOrder.put("orderNumber", order.getNumber());
        kitchenOrder.put("orderTime", order.getOrderTime() != null ? order.getOrderTime().format(DATE_FORMAT) : "");
        kitchenOrder.put("orderType", getOrderTypeText(order.getSource() != null ? order.getSource().toString() : ""));

        // Items with flavors
        StringBuilder items = new StringBuilder();
        for (OrderDetail detail : details) {
            items.append(detail.getName());
            if (detail.getDishFlavor() != null && !detail.getDishFlavor().isEmpty()) {
                items.append("(").append(detail.getDishFlavor()).append(")");
            }
            items.append(" x").append(detail.getNumber());
            if (detail.getRemark() != null && !detail.getRemark().isEmpty()) {
                items.append(" [").append(detail.getRemark()).append("]");
            }
            items.append("\n");
        }
        kitchenOrder.put("items", items.toString());

        // Remark
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            kitchenOrder.put("remark", "备注：" + order.getRemark());
        }

        // Table info for dine-in
        if (order.getTableName() != null) {
            kitchenOrder.put("tableName", order.getTableName());
        }

        return kitchenOrder;
    }

    /**
     * Generate daily summary receipt
     *
     * @param date         Summary date
     * @param totalOrders  Total orders
     * @param totalRevenue Total revenue
     * @param cashIncome   Cash income
     * @param wechatIncome WeChat income
     * @param alipayIncome Alipay income
     * @return Daily summary receipt
     */
    public Map<String, Object> generateDailySummaryReceipt(String date, int totalOrders,
                                                            BigDecimal totalRevenue, BigDecimal cashIncome,
                                                            BigDecimal wechatIncome, BigDecimal alipayIncome) {
        Map<String, Object> receipt = new HashMap<>();

        receipt.put("title", "日结报表");
        receipt.put("date", date);
        receipt.put("storeName", "瑞吉外卖");

        StringBuilder content = new StringBuilder();
        content.append("--------------------------------\n");
        content.append("日期：").append(date).append("\n");
        content.append("--------------------------------\n");
        content.append("订单总数：").append(totalOrders).append("\n");
        content.append("营业总额：¥").append(totalRevenue.setScale(2)).append("\n");
        content.append("--------------------------------\n");
        content.append("现金收入：¥").append(cashIncome.setScale(2)).append("\n");
        content.append("微信收入：¥").append(wechatIncome.setScale(2)).append("\n");
        content.append("支付宝收入：¥").append(alipayIncome.setScale(2)).append("\n");
        content.append("--------------------------------\n");

        receipt.put("content", content.toString());
        receipt.put("printTime", java.time.LocalDateTime.now().format(DATE_FORMAT));

        return receipt;
    }

    /**
     * Get order type text
     */
    private String getOrderTypeText(String source) {
        if (source == null || source.isEmpty()) return "普通订单";
        switch (Integer.parseInt(source)) {
            case 1: return "堂食订单";
            case 2: return "外卖订单";
            case 3: return "打包订单";
            default: return "普通订单";
        }
    }

    /**
     * Truncate string to specified length
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength);
    }
}


