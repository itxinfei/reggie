package com.reggie.module.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 买赠活动命中结果（内部核价对象）。
 *
 * @author reggie
 * @since 2026-09-24
 */
@Data
@Schema(description = "买赠活动命中结果")
public class GiftMatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "买赠活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "赠品菜品ID")
    private Long giftDishId;

    @Schema(description = "赠品菜品名称")
    private String giftDishName;

    @Schema(description = "本单触发次数")
    private Integer times;

    @Schema(description = "赠品总数量 = times × 每档赠品数")
    private Integer giftQuantity;
}
