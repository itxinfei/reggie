package com.reggie.module.order.dto;

import javax.validation.constraints.NotNull;

/**
 * 提交订单请求 DTO
 * 仅包含下单所需的地址ID，避免将完整 Orders 实体作为 @Valid 校验目标。
 * 订单金额、用户ID等由 Service 层根据购物车和用户会话自动填充。
 */
public class OrderSubmitDTO {

    @NotNull(message = "地址ID不能为空")
    private Long addressBookId;

    private String remark;

    private String phone;

    private String idempotencyKey;

    public Long getAddressBookId() {
        return addressBookId;
    }

    public void setAddressBookId(Long addressBookId) {
        this.addressBookId = addressBookId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}