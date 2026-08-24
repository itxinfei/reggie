package com.reggie.module.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 平台同步日志查询DTO
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@Schema(description = "平台同步日志查询")
public class PlatformSyncLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "平台类型", example = "MEITUAN")
    private String platformType;

    @Schema(description = "动作类型", example = "PULL")
    private String action;

    @Schema(description = "开始时间", example = "2026-08-24 00:00:00")
    private String startTime;

    @Schema(description = "结束时间", example = "2026-08-24 23:59:59")
    private String endTime;

    @Schema(description = "结果状态：0=成功 1=失败", example = "1")
    private Integer status;
}
