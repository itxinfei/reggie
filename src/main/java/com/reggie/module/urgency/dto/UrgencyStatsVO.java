package com.reggie.module.urgency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 催单统计视图对象
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Schema(description = "催单统计")
public class UrgencyStatsVO {

    @Schema(description = "总催单次数", example = "120")
    private Integer totalUrgency;

    @Schema(description = "今日催单次数", example = "25")
    private Integer todayUrgency;

    @Schema(description = "本周催单次数", example = "88")
    private Integer weekUrgency;

    @Schema(description = "平均响应时间（分钟）", example = "3.5")
    private Double avgResponseTime;
}
