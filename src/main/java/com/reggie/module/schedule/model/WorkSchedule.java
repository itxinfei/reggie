package com.reggie.module.schedule.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班记录实体
 */
@Data
@TableName("work_schedule")
@Schema(description = "排班记录")
public class WorkSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排班ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工ID", example = "1")
    private Long employeeId;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "排班日期", example = "2026-08-17")
    private LocalDate scheduleDate;

    @Schema(description = "班次：0=早班,1=中班,2=晚班,3=全天", example = "3")
    private Integer shift;

    @Schema(description = "班次开始时间", example = "08:00")
    private LocalTime shiftStart;

    @Schema(description = "班次结束时间", example = "20:00")
    private LocalTime shiftEnd;

    @Schema(description = "工作日期字符串", example = "2026-08-17")
    private String workDateStr;

    @Schema(description = "备注", example = "值班")
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
