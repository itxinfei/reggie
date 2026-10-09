package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 开台请求 DTO
 * <p>
 * 顾客入座后，将桌台状态从空闲改为占用，并绑定订单。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@Schema(description = "开台请求")
public class OpenTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "桌台ID不能为空")
    @Schema(description = "桌台ID", required = true, example = "1")
    private Long tableId;

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true, example = "1001")
    private Long orderId;
}
