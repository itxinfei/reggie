package com.reggie.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 结算预览请求（C 端 /api/order/preview 入参）。
 * <p>金额一律不接收前端值，全部由服务端按购物车重核。</p>
 *
 * @author reggie
 * @since 2026-09-24
 */
@Data
@Schema(description = "结算预览请求")
public class CheckoutPreviewRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 修改点（P1-2 自提）：自提单无需地址，去除 @NotNull，改由 Service 按 source 分支校验
    @Schema(description = "收货地址ID（自提单无需传）")
    private Long addressBookId;

    /**
     * 订单来源/履约方式：TAKEOUT-外卖配送，SELF_PICKUP-到店自提。
     * 不传默认 TAKEOUT（向后兼容）。
     */
    @Schema(description = "订单来源：TAKEOUT-外卖配送，SELF_PICKUP-到店自提", example = "TAKEOUT")
    private String source;

    @Schema(description = "用户选择的优惠券ID（不使用为 null）")
    private Long usedCouponId;
}
