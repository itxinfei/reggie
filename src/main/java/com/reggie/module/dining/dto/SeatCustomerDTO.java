package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 安排入座请求 DTO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "安排入座请求")
public class SeatCustomerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "排队记录ID不能为空")
    @Schema(description = "排队记录ID", required = true, example = "1")
    private Long queueId;

    @Schema(description = "桌台ID（可选，关联桌台）", example = "1")
    private Long tableId;
}