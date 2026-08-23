package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 新增系统配置 DTO
 * <p>仅包含前端需要填写的业务字段，tenantId / id / configType 等敏感字段
 * 由服务端通过 BaseContext 自动填充，前端无法篡改租户归属。</p>
 */
@Data
@Schema(description = "新增系统配置请求")
public class SystemConfigSaveDTO {

    @Schema(description = "配置键", example = "delivery_fee")
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "配置值", example = "5.00")
    @NotBlank(message = "配置值不能为空")
    private String configValue;
}