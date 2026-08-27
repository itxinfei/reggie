package com.reggie.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 热销菜品数据传输对象
 * <p>统计指定时间范围内的菜品销量排行</p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热销菜品")
public class TopProductsVO {

    @Schema(description = "菜品ID", example = "1")
    private Long dishId;

    @Schema(description = "菜品名称", example = "鱼香肉丝")
    private String dishName;

    @Schema(description = "菜品所属分类", example = "热菜")
    private String category;

    @Schema(description = "销售数量", example = "286")
    private Long salesCount;

    @Schema(description = "销售额", example = "13156.00")
    private BigDecimal revenue;
}
