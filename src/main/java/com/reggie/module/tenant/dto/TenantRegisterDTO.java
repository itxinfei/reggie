package com.reggie.module.tenant.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 租户注册请求 DTO
 * 仅包含注册所需的店铺信息，避免将完整 Tenant 实体作为 @Valid 校验目标。
 * 字段名与前端表单/历史 API 兼容：shopName、address、contact、phone（门店联系电话）。
 */
public class TenantRegisterDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称不能超过64个字符")
    private String shopName;

    @Size(max = 255, message = "地址不能超过255个字符")
    private String address;

    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contact;

    @Size(max = 20, message = "门店电话不能超过20个字符")
    private String phone;

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}