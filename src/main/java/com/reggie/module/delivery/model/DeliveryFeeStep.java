package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送费阶梯规则实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("delivery_fee_step")
@Schema(description = "配送费阶梯规则")
public class DeliveryFeeStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "起始距离（米）")
    private BigDecimal startDistance;

    @Schema(description = "结束距离（米）")
    private BigDecimal endDistance;

    @Schema(description = "配送费")
    private BigDecimal fee;

    @Schema(description = "每增加距离（米）")
    private BigDecimal incrementDistance;

    @Schema(description = "增加费用")
    private BigDecimal incrementFee;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
