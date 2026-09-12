package com.reggie.module.order.dto;

import javax.validation.constraints.NotNull;

/**
 * 更新订单状态请求 DTO
 * 仅包含订单ID和状态，避免将完整 Orders 实体作为 @Valid 校验目标
 */
public class OrderUpdateStatusDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    @NotNull(message = "订单状态不能为空")
    private Integer status;

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

    /**
     * 获取 status。
     * @return 返回结果
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置 status。
     * @param status 参数 status
     */
    public void setStatus(Integer status) {
        this.status = status;
    }
}