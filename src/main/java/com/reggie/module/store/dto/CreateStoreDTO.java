package com.reggie.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建门店请求 DTO
 * 白名单字段，防止 mass assignment 攻击
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class CreateStoreDTO {

    @Schema(description = "门店编码", required = true)
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 32, message = "门店编码不能超过32个字符")
    private String storeCode;

    @Schema(description = "门店名称", required = true)
    @NotBlank(message = "门店名称不能为空")
    @Size(max = 64, message = "门店名称不能超过64个字符")
    private String storeName;

    @Schema(description = "门店类型：1-直营总店 2-直营分店 3-加盟店")
    @NotNull(message = "门店类型不能为空")
    private Integer storeType;

    @Schema(description = "营业时间", example = "09:00-22:00")
    @Size(max = 100, message = "营业时间不能超过100个字符")
    private String businessHours;

    @Schema(description = "配送半径(米)")
    @NotNull(message = "配送半径不能为空")
    private Integer deliveryRadius;

    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contactPerson;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    @Schema(description = "门店地址", required = true)
    @NotBlank(message = "门店地址不能为空")
    @Size(max = 255, message = "门店地址不能超过255个字符")
    private String address;

    @Schema(description = "管理员账号", required = true)
    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 64, message = "管理员账号不能超过64个字符")
    private String adminUsername;

    @Schema(description = "管理员密码", required = true)
    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, max = 128, message = "管理员密码长度在6-128之间")
    private String adminPassword;
}
