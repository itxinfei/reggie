package com.reggie.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 同步分类请求 DTO
 * 白名单字段，防止 mass assignment 攻击
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class SyncCategoriesDTO {

    @Schema(description = "源门店tenantId", required = true)
    @NotNull(message = "源门店ID不能为空")
    private Long sourceTenantId;

    @Schema(description = "目标门店tenantId", required = true)
    @NotNull(message = "目标门店ID不能为空")
    private Long targetTenantId;

    @Schema(description = "操作人ID", required = true)
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
}
