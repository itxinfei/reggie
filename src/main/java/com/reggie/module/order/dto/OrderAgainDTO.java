package com.reggie.module.order.dto;

import javax.validation.constraints.NotNull;

/**
 * 再来一单请求 DTO
 * 仅包含订单ID，避免将完整 Orders 实体作为 @Valid 校验目标
 */
public class OrderAgainDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    /**
     * 获取 id。
     * @return 返回结果
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置 id。
     * @param id 参数 id
     */
    public void setId(Long id) {
        this.id = id;
    }
}