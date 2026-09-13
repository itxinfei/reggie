package com.reggie.module.billing.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaaS 套餐方案（平台级全局表，无 tenant_id，所有租户共享同一份套餐目录）
 *
 * @author reggie
 * @since 2026-09-12
 */
@Data
@TableName("billing_plan")
@Schema(description = "SaaS套餐方案")
public class BillingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：下架 */
    public static final int STATUS_OFF = 0;
    /** 状态：上架 */
    public static final int STATUS_ON = 1;

    @Schema(description = "套餐ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "套餐编码（BASIC/STANDARD/PREMIUM）", example = "STANDARD", required = true)
    @NotBlank(message = "套餐编码不能为空")
    private String planCode;

    @Schema(description = "套餐名称", example = "标准版", required = true)
    @NotBlank(message = "套餐名称不能为空")
    private String planName;

    @Schema(description = "月付价格", example = "299.00", required = true)
    @NotNull(message = "月付价格不能为空")
    private BigDecimal price;

    @Schema(description = "年付价格（为空时按 月价×12 计算）", example = "2990.00")
    private BigDecimal annualPrice;

    @Schema(description = "最大门店数（-1 表示不限）", example = "3")
    private Integer maxStores;

    @Schema(description = "最大员工数（-1 表示不限）", example = "20")
    private Integer maxEmployees;

    @Schema(description = "功能权益说明（逗号分隔的功能点）", example = "聚合外卖,会员营销,库存管理")
    private String features;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0=下架，1=上架", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
