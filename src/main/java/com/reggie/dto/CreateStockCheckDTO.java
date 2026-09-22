package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建盘点单请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CreateStockCheckDTO {

    @Schema(description = "操作人", required = true, example = "张三")
    @NotNull(message = "操作人不能为空")
    private String operator;

    @Schema(description = "备注", example = "月度盘点")
    private String remark;

    @Schema(description = "凭证图片（逗号分隔相对路径，最多5张）")
    @Size(max = 1000, message = "凭证图片信息过长")
    private String voucherImages;
}
