package com.reggie.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 更新订单状态DTO
 */
@Data
@Schema(description = "订单状态更新信息")
public class OrderUpdateStatusDTO {

    @Schema(description = "订单状态", required = true)
    @NotNull(message = "订单状态不能为空")
    private Integer status;
}
