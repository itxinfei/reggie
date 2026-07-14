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
 * 库存盘点
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("stock_check")
@Schema(description = "库存盘点单")
public class StockCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "盘点单ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "盘点单号", example = "SC20260709001")
    private String checkNo;

    @Schema(description = "状态：PENDING=盘点中，COMPLETED=已完成，CANCELLED=已取消", example = "PENDING")
    private String status;

    @Schema(description = "总差异金额（元）", example = "120.50")
    private BigDecimal totalDiffAmount;

    @Schema(description = "操作员", example = "张三")
    private String operator;

    @Schema(description = "备注", example = "月度盘点")
    private String remark;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    private LocalDateTime updatedTime;
}
