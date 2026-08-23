package com.reggie.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量更新门店状态请求 DTO
 * 白名单字段，防止 mass assignment 攻击
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class BatchUpdateStatusDTO {

    @Schema(description = "门店tenantId列表", required = true)
    @NotNull(message = "门店ID列表不能为空")
    @NotEmpty(message = "门店ID列表不能为空")
    @Size(max = 100, message = "批量操作门店数量不能超过100个")
    private List<Long> tenantIds;

    @Schema(description = "目标状态：0-停用 1-启用", required = true)
    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "状态值必须为0或1")
    @Max(value = 1, message = "状态值必须为0或1")
    private Integer status;
}
