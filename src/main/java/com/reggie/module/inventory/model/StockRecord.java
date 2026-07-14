package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存记录
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("stock_record")
@Schema(description = "库存变动记录")
public class StockRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "物料ID", example = "1")
    private Long materialId;

    @Schema(description = "变动类型：IN=入库，OUT=出库，ADJUST=调整", example = "IN")
    private String type;

    @Schema(description = "变动数量", example = "20.00")
    private BigDecimal qty;

    @Schema(description = "单价（元）", example = "3.50")
    private BigDecimal unitPrice;

    @Schema(description = "变动总金额（元）", example = "70.00")
    private BigDecimal totalAmount;

    @Schema(description = "关联业务ID（采购单/盘点单等）", example = "1")
    private Long bizId;

    @Schema(description = "备注", example = "采购入库")
    private String remark;

    @Schema(description = "操作员", example = "张三")
    private String operator;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;
}
