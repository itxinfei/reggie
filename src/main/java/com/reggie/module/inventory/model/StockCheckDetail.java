package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存盘点明细
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("stock_check_detail")
@Schema(description = "库存盘点明细")
public class StockCheckDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "盘点明细ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "盘点单ID", example = "1")
    private Long checkId;

    @Schema(description = "物料ID", example = "1")
    private Long materialId;

    @Schema(description = "账面数量", example = "50.00")
    private BigDecimal bookQty;

    @Schema(description = "实际盘点数量", example = "48.00")
    private BigDecimal actualQty;

    @Schema(description = "差异数量（实际-账面）", example = "-2.00")
    private BigDecimal diffQty;

    @Schema(description = "备注", example = "损耗2斤")
    private String remark;
}
