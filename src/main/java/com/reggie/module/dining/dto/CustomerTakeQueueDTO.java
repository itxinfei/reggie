package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * C 端顾客排队取号请求
 *
 * @author reggie
 * @since 2026-09-29
 */
@Data
@Schema(description = "顾客排队取号请求")
public class CustomerTakeQueueDTO {

    /** 就座人数 */
    @NotNull(message = "请选择就餐人数")
    @Min(value = 1, message = "就餐人数至少 1 人")
    @Max(value = 20, message = "就餐人数不能超过 20 人")
    @Schema(description = "就座人数", example = "2")
    private Integer seatCount;
}
