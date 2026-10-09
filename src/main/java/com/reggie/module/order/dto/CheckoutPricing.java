package com.reggie.module.order.dto;

import com.reggie.module.marketing.dto.GiftMatch;
import com.reggie.module.marketing.dto.NewCustomerEvaluation;
import com.reggie.module.order.model.OrderDetail;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 结算核价内部承载对象（不出网）。
 * <p>同时携带出网视图、真实下单所需的花钱/赠品明细、各项营销命中，
 * 供 {@code submit} 落库与 C 端预览共用同一计算核心。</p>
 *
 * @author reggie
 * @since 2026-09-24
 */
@Data
public class CheckoutPricing implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 出网视图 */
    private CheckoutPreviewDTO view;

    /** 花钱明细（已含秒杀替换价，orderId 已回填） */
    private List<OrderDetail> payableDetails;

    /** 赠品明细（amount=0，已按 giftDishId 合并） */
    private List<OrderDetail> giftDetails;

    /** 满减命中描述（沿用现结构 campaignId/ruleId/discount/goodsAmount） */
    private Map<String, Object> frHit;

    /** 新客立减命中（null=未命中） */
    NewCustomerEvaluation ncHit;

    /** 本单秒杀行 */
    private List<FlashHitLine> flashHits;

    /** 本单买赠命中 */
    private List<GiftMatch> giftHits;

    /** 配送费（落库用） */
    private BigDecimal deliveryFee;

    /**
     * 秒杀命中行（落库扣库存与参与记录用）。
     */
    @Data
    public static class FlashHitLine implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long flashSaleId;

        private String flashSaleName;

        private Long dishId;

        private String dishName;

        private Integer quantity;

        /** 本行原价合计 */
        private BigDecimal originalTotal;

        /** 本行秒杀应付合计 */
        private BigDecimal payableTotal;
    }
}
