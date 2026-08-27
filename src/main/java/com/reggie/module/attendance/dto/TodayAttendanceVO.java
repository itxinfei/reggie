package com.reggie.module.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 今日考勤 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "今日考勤")
public class TodayAttendanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日日期")
    private String date;

    @Schema(description = "已到岗人数")
    private Integer checkedInCount;

    @Schema(description = "未到岗人数")
    private Integer notCheckedInCount;

    @Schema(description = "请假人数")
    private Integer leaveCount;

    @Schema(description = "已到岗员工列表")
    private List<TodayEmployeeVO> checkedInEmployees;

    @Schema(description = "未到岗员工列表")
    private List<TodayEmployeeVO> notCheckedInEmployees;

    @Schema(description = "请假员工列表")
    private List<TodayEmployeeVO> leaveEmployees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayEmployeeVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "员工ID")
        private Long employeeId;

        @Schema(description = "员工姓名")
        private String employeeName;

        @Schema(description = "签到时间")
        private String checkInTime;

        @Schema(description = "考勤状态")
        private Integer status;
    }
}