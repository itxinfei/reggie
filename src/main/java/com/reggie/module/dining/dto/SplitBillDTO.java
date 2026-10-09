package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

/**
 * 分账请求 DTO（AA 制）
 * <p>
 * 将订单拆分为多份子订单分别结算，适用于多人 AA 场景。
 * </p>
 *
 * @author reggie
 * @since 2026-08-31
 */
@Data
@Schema(description = "AA 分账请求")
public class SplitBillDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "主订单ID", required = true, example = "100")
    private Long orderId;

    @NotNull
    @Positive(message = "分账份数必须大于 0")
    @Schema(description = "分账份数", required = true, example = "3")
    private Integer parts;

    @Schema(description = "备注", example = "三人 AA")
    private String remark;
}
