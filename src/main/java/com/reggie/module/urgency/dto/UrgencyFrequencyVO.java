package com.reggie.module.urgency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 催单频率控制视图对象
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Schema(description = "催单频率控制")
public class UrgencyFrequencyVO {

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "今日催单次数", example = "2")
    private Integer todayCount;

    @Schema(description = "最大允许次数", example = "3")
    private Integer maxAllowed;

    @Schema(description = "是否可催单", example = "true")
    private Boolean canUrgency;

    @Schema(description = "剩余可催单次数", example = "1")
    private Integer remainCount;
}
