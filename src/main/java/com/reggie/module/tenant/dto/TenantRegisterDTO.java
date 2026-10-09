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

    /**
     * 获取 shop name。
     * @return 返回结果
     */
    public String getShopName() {
        return shopName;
    }

    /**
     * 设置 shop name。
     * @param shopName 参数 shopName
     */
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    /**
     * 获取 address。
     * @return 返回结果
     */
    public String getAddress() {
        return address;
    }

    /**
     * 设置 address。
     * @param address 参数 address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 获取 contact。
     * @return 返回结果
     */
    public String getContact() {
        return contact;
    }

    /**
     * 设置 contact。
     * @param contact 参数 contact
     */
    public void setContact(String contact) {
        this.contact = contact;
    }

    /**
     * 获取 phone。
     * @return 返回结果
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置 phone。
     * @param phone 参数 phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }
}