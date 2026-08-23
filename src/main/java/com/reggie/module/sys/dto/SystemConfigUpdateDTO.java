package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新系统配置 DTO
 * <p>前端仅提交 id + configKey + configValue 三个业务字段。
 * tenantId 由服务端通过 BaseContext 校验归属，不允许前端指定。</p>
 */
@Data
@Schema(description = "更新系统配置请求")
public class SystemConfigUpdateDTO {

    @Schema(description = "配置ID")
    @NotNull(message = "配置ID不能为空")
    private Long id;

    @Schema(description = "配置键（用于校验归属）")
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "新配置值")
    @NotBlank(message = "配置值不能为空")
    private String configValue;
}