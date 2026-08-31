package com.reggie.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 提交订单请求 DTO
 * 仅包含下单所需的地址ID，避免将完整 Orders 实体作为 @Valid 校验目标。
 * 订单金额、用户ID等由 Service 层根据购物车和用户会话自动填充。
 *
 * <p>修改点（2026-08-30）：C 端 add-order.html 实际提交了 6 个字段，
 * 而本 DTO 只有 4 个，导致 payMethod / expectDeliveryTime / usedCouponId
 * 被 Jackson 静默忽略——用户选的优惠券、预约配送时间、支付方式全部不生效，
 * 且不报错不告警。现补齐三个字段并在 Controller 透传到 Orders
 * （Orders 实体本就有这三个字段，此前只是没接上）。</p>
 *
 * <p><b>安全说明</b>：C 端同时提交了 amount，此处<b>刻意不接收</b>。
 * 订单金额必须由后端按购物车重算，接收前端金额等于允许客户端篡改价格。
 * 若需防止误传，前端应移除该参数；后端即便收到也会忽略。</p>
 */
@Data
public class OrderSubmitDTO {

    @NotNull(message = "地址ID不能为空")
    @Schema(description = "收货地址ID", required = true)
    private Long addressBookId;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "幂等令牌，防止重复下单")
    private String idempotencyKey;

    /**
     * 支付方式（与 Orders.payMethod 权威枚举一致）：
     * 1=现金，2=微信支付，3=支付宝，4=银行卡，5=会员储值，6=货到付款
     */
    @Schema(description = "支付方式：1=现金，2=微信支付，3=支付宝，4=银行卡，5=会员储值，6=货到付款", example = "2")
    private Integer payMethod;

    /**
     * 期望配送时间（预约配送），字符串格式由前端与后端约定（如 "2026-08-30 18:30"）
     */
    @Schema(description = "期望配送时间（预约配送，选填）", example = "2026-08-30 18:30")
    private String expectDeliveryTime;

    /**
     * 本单使用的优惠券ID（对应 coupon_user.id，由后端核销）
     */
    @Schema(description = "使用的优惠券ID（选填）", example = "1")
    private Long usedCouponId;
}
