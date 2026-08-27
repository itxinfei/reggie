package com.reggie.module.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 员工考勤日历 VO
 * 某员工某月的每日考勤状态
 */
@Data
@Schema(description = "考勤日历项")
public class AttendanceCalendarVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期", example = "2026-08-17")
    private String date;

    @Schema(description = "星期几(1-7)", example = "2")
    private Integer weekday;

    @Schema(description = "考勤状态：0=缺勤,1=正常,2=迟到,3=早退,4=请假,5=出差")
    private Integer status;

    @Schema(description = "签到时间", example = "2026-08-17 08:30:00")
    private String checkInTime;

    @Schema(description = "签退时间", example = "2026-08-17 18:00:00")
    private String checkOutTime;

    @Schema(description = "工时(小时)", example = "8.0")
    private Double workHours;

    @Schema(description = "备注")
    private String remark;
}