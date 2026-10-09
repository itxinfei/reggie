package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 扣减余额请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class DeductBalanceDTO {

    @Schema(description = "会员ID", required = true, example = "1")
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "扣减金额", required = true, example = "50.00")
    @NotNull(message = "扣减金额不能为空")
    @DecimalMin(value = "0.01", message = "扣减金额必须大于0")
    private BigDecimal amount;
}
