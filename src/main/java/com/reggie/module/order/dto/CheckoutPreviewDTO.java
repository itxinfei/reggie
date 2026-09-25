package com.reggie.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览出网视图（C 端订单确认页单一数据源）。
 * <p>由只读取计算核心产出，与真实下单 {@code /order/submit} 同源，前端不得本地另算价格。</p>
 *
 * @author reggie
 * @since 2026-09-24
 */
@Data
@Schema(description = "结算预览视图")
public class CheckoutPreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "花钱商品明细（已含秒杀替换价）")
    private List<DetailLine> details;

    @Schema(description = "赠品明细（0 元）")
    private List<GiftLine> gifts;

    @Schema(description = "商品应付金额（秒杀替换后，整单优惠唯一基数）")
    private BigDecimal goodsAmount;

    @Schema(description = "商品原价合计（按菜品/套餐原价）")
    private BigDecimal originalGoodsAmount;

    @Schema(description = "秒杀差价合计（恒等式 goodsAmount = originalGoodsAmount - flashSavings）")
    private BigDecimal flashSavings;

    @Schema(description = "满减命中（未命中 amount=0、campaignId 为 null）")
    private ActivityHit fullReduction;

    @Schema(description = "新客立减命中（未命中 amount=0、campaignId 为 null）")
    private ActivityHit newCustomer;

    @Schema(description = "优惠券命中（campaignId=用户券记录ID，未使用为 null）")
    private ActivityHit coupon;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "已优惠合计 = 秒杀差价 + 满减 + 新客立减 + 券")
    private BigDecimal totalDiscount;

    @Schema(description = "应付金额（含配送费，下限 0）")
    private BigDecimal payAmount;

    @Schema(description = "是否未达起送价")
    private boolean belowMinOrder;

    @Schema(description = "是否已做配送范围校验（门店或地址坐标缺失为 false）")
    private boolean rangeChecked;

    @Schema(description = "是否在配送范围内（仅 rangeChecked=true 时有意义）")
    private boolean inRange;

    @Schema(description = "不可下单原因（秒杀快照库存不足/停售等软提示，可下单为 null）")
    private String unavailableReason;

    /**
     * 商品明细行。
     */
    @Data
    @Schema(description = "商品明细行")
    public static class DetailLine implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商品名称")
        private String name;

        @Schema(description = "菜品ID")
        private Long dishId;

        @Schema(description = "套餐ID")
        private Long setmealId;

        @Schema(description = "口味")
        private String dishFlavor;

        @Schema(description = "图片")
        private String image;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "原价单价")
        private BigDecimal originalUnitPrice;

        @Schema(description = "实付单价（秒杀价）")
        private BigDecimal unitPrice;

        @Schema(description = "行金额 = unitPrice × quantity")
        private BigDecimal lineAmount;

        @Schema(description = "秒杀活动ID（非秒杀为 null）")
        private Long flashSaleId;

        @Schema(description = "秒杀活动名称")
        private String flashSaleName;

        @Schema(description = "本行秒杀差价")
        private BigDecimal lineSavings;
    }

    /**
     * 赠品行（按活动列出，便于展示来源）。
     */
    @Data
    @Schema(description = "赠品行")
    public static class GiftLine implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "买赠活动ID")
        private Long activityId;

        @Schema(description = "赠品菜品ID")
        private Long giftDishId;

        @Schema(description = "赠品名称")
        private String name;

        @Schema(description = "赠品数量")
        private Integer quantity;
    }

    /**
     * 整单优惠命中项。
     */
    @Data
    @Schema(description = "整单优惠命中项")
    public static class ActivityHit implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "活动/券 ID（未命中为 null）")
        private Long campaignId;

        @Schema(description = "优惠金额")
        private BigDecimal amount;

        @Schema(description = "展示文案")
        private String copyText;
    }
}
