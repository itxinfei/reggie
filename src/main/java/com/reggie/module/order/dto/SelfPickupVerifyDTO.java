package com.reggie.module.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 店员核销自提订单请求：校验顾客出示的取餐码
 */
@Data
public class SelfPickupVerifyDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    @NotBlank(message = "请输入顾客取餐码")
    private String pickupCode;
}
