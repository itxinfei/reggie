package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 领取优惠券请求DTO
 */
@Data
public class ClaimCouponDTO {

    @Schema(description = "会员ID", required = true, example = "1")
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "优惠券模板ID", required = true, example = "1")
    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;
}
