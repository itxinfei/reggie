package com.reggie.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 再来一单DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "再来一单信息")
public class OrderAgainDTO {

    @Schema(description = "订单ID", required = true)
    private Long id;
}
