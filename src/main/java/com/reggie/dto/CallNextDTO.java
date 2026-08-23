package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 叫号请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CallNextDTO {

    @Schema(description = "座位数（可选，按座位数筛选）", example = "4")
    @Min(value = 1, message = "座位数必须大于0")
    private Integer seatCount;
}
