package com.reggie.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 店长派单请求：将待接单订单指派给指定骑手。
 *
 * @author reggie
 * @since 2026-09-23
 */
@Data
@Schema(description = "店长派单请求")
public class OrderDispatchDTO {

    @Schema(description = "订单ID", required = true)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "骑手ID", required = true)
    @NotNull(message = "骑手ID不能为空")
    private Long riderId;
}
