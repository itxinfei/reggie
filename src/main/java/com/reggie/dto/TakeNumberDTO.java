package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 排队取号请求DTO
 */
@Data
public class TakeNumberDTO {

    @Schema(description = "座位数", required = true, example = "4")
    @Min(value = 1, message = "座位数必须大于0")
    private Integer seatCount;

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
