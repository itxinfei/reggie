package com.reggie.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 再来一单DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Schema(description = "再来一单信息")
public class OrderAgainDTO {

    @Schema(description = "订单ID", required = true)
    private Long id;
}
