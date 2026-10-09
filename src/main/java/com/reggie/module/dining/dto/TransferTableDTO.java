package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 转台请求 DTO
 * <p>
 * 顾客换桌，订单与新桌台绑定，原桌台释放。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@Schema(description = "转台请求")
public class TransferTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "原桌台ID不能为空")
    @Schema(description = "原桌台ID", required = true, example = "1")
    private Long fromTableId;

    @NotNull(message = "目标桌台ID不能为空")
    @Schema(description = "目标桌台ID", required = true, example = "2")
    private Long toTableId;
}
