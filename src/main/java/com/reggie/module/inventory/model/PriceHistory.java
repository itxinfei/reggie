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
 * 价格历史记录
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("price_history")
@Schema(description = "价格历史记录")
public class PriceHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "价格历史ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "物料ID", example = "1")
    private Long materialId;

    @Schema(description = "旧价格（元）", example = "3.50")
    private BigDecimal oldPrice;

    @Schema(description = "新价格（元）", example = "3.80")
    private BigDecimal newPrice;

    @Schema(description = "变动原因", example = "市场涨价")
    private String changeReason;

    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    @Schema(description = "创建时间", example = "2026-09-01 10:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
