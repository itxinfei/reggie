package com.reggie.module.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 本周考勤汇总 VO
 */
@Data
@Schema(description = "本周考勤汇总")
public class WeekSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "周起始日期", example = "2026-08-17")
    private String weekStart;

    @Schema(description = "周结束日期", example = "2026-08-23")
    private String weekEnd;

    @Schema(description = "总出勤人次")
    private Integer totalAttendance;

    @Schema(description = "正常出勤人次")
    private Integer normalCount;

    @Schema(description = "迟到人次")
    private Integer lateCount;

    @Schema(description = "早退人次")
    private Integer earlyLeaveCount;

    @Schema(description = "缺勤人次")
    private Integer absentCount;

    @Schema(description = "请假人次")
    private Integer leaveCount;

    @Schema(description = "出差人次")
    private Integer businessTripCount;

    @Schema(description = "员工总数")
    private Integer employeeTotal;

    @Schema(description = "出勤率(%)")
    private Double attendanceRate;

    @Schema(description = "平均工时(小时)")
    private Double avgWorkHours;
}