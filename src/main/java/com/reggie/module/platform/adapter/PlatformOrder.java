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

    /**
     * 获取 raw json。
     * @return 返回结果
     */
    public String getRawJson() {
        return rawJson;
    }

    /**
     * 设置 raw json。
     * @param rawJson 参数 rawJson
     */
    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    /**
     * 获取 platform order id。
     * @return 返回结果
     */
    public String getPlatformOrderId() {
        return platformOrderId;
    }

    /**
     * 设置 platform order id。
     * @param platformOrderId 参数 platformOrderId
     */
    public void setPlatformOrderId(String platformOrderId) {
        this.platformOrderId = platformOrderId;
    }

    /**
     * 获取 platform status。
     * @return 返回结果
     */
    public String getPlatformStatus() {
        return platformStatus;
    }

    /**
     * 设置 platform status。
     * @param platformStatus 参数 platformStatus
     */
    public void setPlatformStatus(String platformStatus) {
        this.platformStatus = platformStatus;
    }

    /**
     * 获取 amount。
     * @return 返回结果
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 设置 amount。
     * @param amount 参数 amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 获取 customer name。
     * @return 返回结果
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * 设置 customer name。
     * @param customerName 参数 customerName
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * 获取 customer phone。
     * @return 返回结果
     */
    public String getCustomerPhone() {
        return customerPhone;
    }

    /**
     * 设置 customer phone。
     * @param customerPhone 参数 customerPhone
     */
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    /**
     * 获取 address。
     * @return 返回结果
     */
    public String getAddress() {
        return address;
    }

    /**
     * 设置 address。
     * @param address 参数 address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 获取 remark。
     * @return 返回结果
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置 remark。
     * @param remark 参数 remark
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取 order time。
     * @return 返回结果
     */
    public String getOrderTime() {
        return orderTime;
    }

    /**
     * 设置 order time。
     * @param orderTime 参数 orderTime
     */
    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    /**
     * 获取 items。
     * @return 返回结果
     */
    public List<OrderItem> getItems() {
        return items;
    }

    /**
     * 设置 items。
     * @param items 参数 items
     */
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

        /**
         * 获取 platform dish id。
         * @return 返回结果
         */
        public String getPlatformDishId() {
            return platformDishId;
        }
        /**
         * 设置 platform dish id。
         * @param platformDishId 参数 platformDishId
         */
        public void setPlatformDishId(String platformDishId) {
            this.platformDishId = platformDishId;
        }
        /**
         * 获取 dish name。
         * @return 返回结果
         */
        public String getDishName() {
            return dishName;
        }
        /**
         * 设置 dish name。
         * @param dishName 参数 dishName
         */
        public void setDishName(String dishName) {
            this.dishName = dishName;
        }
        /**
         * 获取 quantity。
         * @return 返回结果
         */
        public Integer getQuantity() {
            return quantity;
        }
        /**
         * 设置 quantity。
         * @param quantity 参数 quantity
         */
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        /**
         * 获取 price。
         * @return 返回结果
         */
        public BigDecimal getPrice() {
            return price;
        }
        /**
         * 设置 price。
         * @param price 参数 price
         */
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        /**
         * 获取 flavor。
         * @return 返回结果
         */
        public String getFlavor() {
            return flavor;
        }
        /**
         * 设置 flavor。
         * @param flavor 参数 flavor
         */
        public void setFlavor(String flavor) {
            this.flavor = flavor;
        }
    }
}
