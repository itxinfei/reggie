package com.reggie.module.delivery.dto;

import com.reggie.module.order.model.OrderDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 骑手任务视图对象。
 * <p>
 * 同时承载订单基础信息、取餐门店、送餐地址、商品明细与各动作时间戳，
 * 供骑手端列表与详情使用。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Data
@Schema(description = "骑手任务")
public class RiderTaskVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String number;

    @Schema(description = "订单状态：2待接单 3配送中 4已完成")
    private Integer status;

    @Schema(description = "接单骑手ID（NULL=在抢单大厅，未派单/未抢单）")
    private Long riderId;

    @Schema(description = "订单金额")
    private BigDecimal amount;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "期望送达时间")
    private String expectDeliveryTime;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    // ---- 取餐门店 ----

    @Schema(description = "取餐门店名称")
    private String storeName;

    @Schema(description = "取餐门店地址")
    private String storeAddress;

    @Schema(description = "取餐门店电话")
    private String storePhone;

    @Schema(description = "取餐门店经度")
    private BigDecimal storeLongitude;

    @Schema(description = "取餐门店纬度")
    private BigDecimal storeLatitude;

    // ---- 送餐地址（订单快照）----

    @Schema(description = "收货人")
    private String consignee;

    @Schema(description = "收货人电话")
    private String phone;

    @Schema(description = "收货地址")
    private String address;

    @Schema(description = "收货经度（来自地址簿）")
    private BigDecimal destLongitude;

    @Schema(description = "收货纬度（来自地址簿）")
    private BigDecimal destLatitude;

    // ---- 商品明细 ----

    @Schema(description = "商品明细")
    private List<OrderDetail> items;

    // ---- 动作时间戳 ----

    @Schema(description = "骑手接单时间")
    private LocalDateTime acceptTime;

    @Schema(description = "骑手取餐时间")
    private LocalDateTime pickupTime;

    @Schema(description = "骑手送达时间")
    private LocalDateTime deliverTime;
}
