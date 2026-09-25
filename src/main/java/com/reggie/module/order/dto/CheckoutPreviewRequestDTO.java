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

    @Schema(description = "收货地址ID", required = true)
    @NotNull(message = "请选择收货地址")
    private Long addressBookId;

    @Schema(description = "用户选择的优惠券ID（不使用为 null）")
    private Long usedCouponId;
}
