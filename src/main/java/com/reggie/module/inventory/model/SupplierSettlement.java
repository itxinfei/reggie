package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商结算单
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("supplier_settlement")
@Schema(description = "供应商结算单")
public class SupplierSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "结算单ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    @Schema(description = "结算周期", example = "202609")
    private String period;

    @Schema(description = "总金额（元）", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已付金额（元）", example = "500.00")
    private BigDecimal paidAmount;

    @Schema(description = "状态：PENDING=待付款，PAID=已付款", example = "PENDING")
    private String status;

    @Schema(description = "创建时间", example = "2026-09-01 10:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-09-01 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
