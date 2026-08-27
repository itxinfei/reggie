package com.reggie.module.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 异常考勤记录 VO
 */
@Data
@Schema(description = "异常考勤记录")
public class AbnormalStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "考勤记录ID")
    private Long id;

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "考勤日期")
    private String date;

    @Schema(description = "考勤状态：0=缺勤,2=迟到,3=早退")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "签到时间")
    private String checkInTime;

    @Schema(description = "签退时间")
    private String checkOutTime;

    @Schema(description = "工时(小时)")
    private BigDecimal workHours;
}