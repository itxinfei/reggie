package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 修改桌台状态请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class ChangeTableStatusDTO {

    @Schema(description = "桌台ID", required = true, example = "1")
    @NotNull(message = "桌台ID不能为空")
    private Long id;

    @Schema(description = "状态", required = true, example = "available")
    @NotNull(message = "状态不能为空")
    private String status;
}
