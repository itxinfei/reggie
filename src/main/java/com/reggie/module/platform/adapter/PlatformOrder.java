package com.reggie.module.platform.adapter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台标准化订单 DTO
 * <p>
 * 用于统一各平台订单数据结构，供平台无关逻辑使用。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
public class PlatformOrder {

    /** 平台订单号（各平台原始订单号，唯一键） */
    private String platformOrderId;

    /** 订单状态（平台原始状态值） */
    private String platformStatus;

    /** 订单金额 */
    private BigDecimal amount;

    /** 顾客姓名 */
    private String customerName;

    /** 顾客电话 */
    private String customerPhone;

    /** 收货地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 下单时间（ISO格式字符串） */
    private String orderTime;

    /** 菜品明细 */
    private List<OrderItem> items;

    /** 平台原始订单 JSON（用于排查与字段补全，由适配器在解析后回填） */
    private String rawJson;

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getPlatformOrderId() {
        return platformOrderId;
    }

    public void setPlatformOrderId(String platformOrderId) {
        this.platformOrderId = platformOrderId;
    }

    public String getPlatformStatus() {
        return platformStatus;
    }

    public void setPlatformStatus(String platformStatus) {
        this.platformStatus = platformStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    /**
     * 订单明细
     */
    public static class OrderItem {
        /** 平台菜品ID */
        private String platformDishId;
        /** 菜品名称 */
        private String dishName;
        /** 数量 */
        private Integer quantity;
        /** 单价 */
        private BigDecimal price;
        /** 口味/规格 */
        private String flavor;

        public String getPlatformDishId() {
            return platformDishId;
        }
        public void setPlatformDishId(String platformDishId) {
            this.platformDishId = platformDishId;
        }
        public String getDishName() {
            return dishName;
        }
        public void setDishName(String dishName) {
            this.dishName = dishName;
        }
        public Integer getQuantity() {
            return quantity;
        }
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        public BigDecimal getPrice() {
            return price;
        }
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        public String getFlavor() {
            return flavor;
        }
        public void setFlavor(String flavor) {
            this.flavor = flavor;
        }
    }
}
