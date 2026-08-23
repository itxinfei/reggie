package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量延期优惠券请求 DTO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "批量延期优惠券请求")
public class BatchExtendCouponDTO {

    @Schema(description = "用户优惠券ID列表", required = true, example = "[1, 2, 3]")
    @NotEmpty(message = "用户优惠券ID列表不能为空")
    @Size(max = 200, message = "用户优惠券ID列表不能超过200个")
    private List<Long> couponUserIds;

    @Schema(description = "延长天数", required = true, example = "7")
    @Positive(message = "延长天数必须大于0")
    private Integer extendDays;
}