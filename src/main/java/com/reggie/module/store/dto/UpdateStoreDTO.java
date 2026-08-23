package com.reggie.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 更新门店信息请求 DTO
 * 白名单字段，防止 mass assignment 攻击
 * 注意：passwordType（Tenant）和 parentTenantId（StoreInfo）不在白名单中
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class UpdateStoreDTO {

    @Schema(description = "门店名称")
    @Size(max = 64, message = "门店名称不能超过64个字符")
    private String storeName;

    @Schema(description = "门店地址")
    @Size(max = 255, message = "门店地址不能超过255个字符")
    private String address;

    @Schema(description = "门店状态：0-停用 1-启用")
    @Min(value = 0, message = "状态值必须为0或1")
    @Max(value = 1, message = "状态值必须为0或1")
    private Integer status;

    @Schema(description = "门店编码")
    @Size(max = 32, message = "门店编码不能超过32个字符")
    private String storeCode;

    @Schema(description = "门店类型：1-直营总店 2-直营分店 3-加盟店")
    @Min(value = 1, message = "门店类型无效")
    @Max(value = 3, message = "门店类型无效")
    private Integer storeType;

    @Schema(description = "营业时间", example = "09:00-22:00")
    @Size(max = 100, message = "营业时间不能超过100个字符")
    private String businessHours;

    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contactPerson;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    @Schema(description = "配送半径(米)")
    @Min(value = 0, message = "配送半径不能小于0")
    private Integer deliveryRadius;

    @Schema(description = "最低起送金额")
    private BigDecimal minDeliveryAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "是否支持外卖：0-否 1-是")
    @Min(value = 0, message = "外卖开关必须为0或1")
    @Max(value = 1, message = "外卖开关必须为0或1")
    private Integer isDeliveryEnabled;

    @Schema(description = "是否支持堂食：0-否 1-是")
    @Min(value = 0, message = "堂食开关必须为0或1")
    @Max(value = 1, message = "堂食开关必须为0或1")
    private Integer isDineInEnabled;
}
