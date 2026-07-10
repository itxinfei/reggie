package com.reggie.module.store.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店扩展信息
 * 在Tenant表基础上补充门店运营信息，实现总部-分店数据隔离
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("store_info")
public class StoreInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 门店类型 - 直营总店 */
    public static final int TYPE_HEADQUARTER = 1;
    /** 门店类型 - 直营分店 */
    public static final int TYPE_DIRECT_BRANCH = 2;
    /** 门店类型 - 加盟店 */
    public static final int TYPE_FRANCHISE = 3;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属租户/门店ID，关联tenant表 */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /** 门店编码 */
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 32, message = "门店编码不能超过32个字符")
    private String storeCode;

    /** 门店类型 */
    private Integer storeType;

    /** 上级总店tenantId */
    private Long parentTenantId;

    /** 营业时间 */
    @Size(max = 100, message = "营业时间不能超过100个字符")
    private String businessHours;

    /** 配送半径(米) */
    private Integer deliveryRadius;

    /** 最低起送金额 */
    private BigDecimal minDeliveryAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 是否支持外卖 0:否 1:是 */
    private Integer isDeliveryEnabled;

    /** 是否支持堂食 0:否 1:是 */
    private Integer isDineInEnabled;

    /** 门店联系人 */
    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contactPerson;

    /** 门店联系电话 */
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
