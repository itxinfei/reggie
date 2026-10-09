package com.reggie.module.attendance.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体
 */
@Data
@TableName("attendance")
@Schema(description = "考勤记录")
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "考勤ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工ID", example = "1")
    private Long employeeId;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "考勤日期", example = "2026-08-17")
    private LocalDate date;

    @Schema(description = "签到时间", example = "2026-08-17 08:30:00")
    private LocalDateTime checkInTime;

    @Schema(description = "签退时间", example = "2026-08-17 18:00:00")
    private LocalDateTime checkOutTime;

    @Schema(description = "考勤状态：0=缺勤,1=正常,2=迟到,3=早退,4=请假,5=出差", example = "1")
    private Integer status;

    @Schema(description = "工时（小时）", example = "8.0")
    private BigDecimal workHours;

    @Schema(description = "备注", example = "会议")
    private String remark;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
