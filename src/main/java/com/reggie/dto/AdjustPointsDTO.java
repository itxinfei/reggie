package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 积分调整请求DTO
 * <p>运营后台手动调整会员积分：正数发放、负数扣减</p>
 *
 * @author reggie
 * @since 2026-08-30
 */
@Data
public class AdjustPointsDTO {

    @Schema(description = "会员ID", required = true, example = "1")
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "积分变动数（正数发放、负数扣减，不可为0）", required = true, example = "100")
    @NotNull(message = "积分变动数不能为空")
    private Integer points;

    @Schema(description = "调整说明", example = "生日关怀赠送")
    private String remark;
}
