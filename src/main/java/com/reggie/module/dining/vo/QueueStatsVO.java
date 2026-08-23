package com.reggie.module.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 排队统计 VO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "排队统计")
public class QueueStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排队总数")
    private Long totalQueues;

    @Schema(description = "等待中数量")
    private Long waitingCount;

    @Schema(description = "已叫号数量")
    private Long calledCount;

    @Schema(description = "已入座数量")
    private Long seatedCount;

    @Schema(description = "已取消数量")
    private Long cancelledCount;
}