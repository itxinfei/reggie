package com.reggie.module.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 桌台统计 VO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "桌台统计")
public class TableStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台总数")
    private Long totalTables;

    @Schema(description = "空闲桌台数")
    private Long freeTables;

    @Schema(description = "占用桌台数")
    private Long occupiedTables;

    @Schema(description = "预留桌台数")
    private Long reservedTables;

    @Schema(description = "清洁中桌台数")
    private Long cleaningTables;
}