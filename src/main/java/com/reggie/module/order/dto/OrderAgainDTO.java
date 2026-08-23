package com.reggie.module.order.dto;

import javax.validation.constraints.NotNull;

/**
 * 再来一单请求 DTO
 * 仅包含订单ID，避免将完整 Orders 实体作为 @Valid 校验目标
 */
public class OrderAgainDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}