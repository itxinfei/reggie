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
 * 物料
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("material")
@Schema(description = "物料（食材/原料）")
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "物料状态：1=正常，0=禁用")
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DISABLED = 0;

    @Schema(description = "物料ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "物料分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "物料名称", example = "土豆", required = true)
    private String name;

    @Schema(description = "单位", example = "斤")
    private String unit;

    @Schema(description = "当前库存数量", example = "50.00")
    private BigDecimal stockQty;

    @Schema(description = "最低库存预警阈值", example = "10.00")
    private BigDecimal minStock;

    @Schema(description = "单价（元）", example = "3.50")
    private BigDecimal unitPrice;

    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    @Schema(description = "条形码", example = "6901234567890")
    private String barcode;

    @Schema(description = "状态：1=正常，0=禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 物料分类名称（关联查询填充，数据库无此列） */
    @TableField(exist = false)
    private String categoryName;

    /** 供应商名称（关联查询填充，数据库无此列） */
    @TableField(exist = false)
    private String supplierName;

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
