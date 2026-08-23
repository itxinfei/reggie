package com.reggie.module.attendance.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 考勤管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@Tag(name = "考勤管理", description = "员工考勤打卡与统计接口")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/week-summary")
    @RequireEmployee
    @Operation(summary = "本周考勤汇总", description = "获取本周总出勤人数/缺勤人数/迟到人数/平均工时等汇总数据")
    @Parameter(name = "weekStart", description = "周起始日期，格式 yyyy-MM-dd", example = "2026-08-17")
    public R<Map<String, Object>> weekSummary(@RequestParam(required = false) String weekStart) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> summary = attendanceService.getWeekSummary(tenantId, weekStart);
        return R.success(summary);
    }

    @GetMapping("/calendar/{employeeId}")
    @RequireEmployee
    @Operation(summary = "考勤日历", description = "获取某员工某月的每日考勤状态日历")
    @Parameter(name = "employeeId", description = "员工ID", required = true)
    public R<Map<String, Object>> calendar(@PathVariable Long employeeId,
                                           @RequestParam(required = false) String month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> calendar = attendanceService.getAttendanceCalendar(employeeId, month, tenantId);
        return R.success(calendar);
    }

    @GetMapping("/today")
    @RequireEmployee
    @Operation(summary = "今日考勤", description = "获取今日已到岗/未到岗/请假员工列表及汇总")
    public R<Map<String, Object>> today() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> today = attendanceService.getTodayAttendance(tenantId);
        return R.success(today);
    }

    @PostMapping("/clockIn")
    @RequireEmployee
    @Operation(summary = "签到打卡", description = "员工签到打卡，自动判断是否迟到")
    public R<Void> clockIn(HttpServletRequest request) {
        Object empIdObj = request.getSession().getAttribute("employee");
        if (empIdObj == null) {
            return R.error("请先登录");
        }
        Long employeeId = (Long) empIdObj;
        return attendanceService.clockIn(employeeId);
    }

    @PostMapping("/clockOut")
    @RequireEmployee
    @Operation(summary = "签退打卡", description = "员工签退打卡，自动判断是否早退")
    public R<Void> clockOut(HttpServletRequest request) {
        Object empIdObj = request.getSession().getAttribute("employee");
        if (empIdObj == null) {
            return R.error("请先登录");
        }
        Long employeeId = (Long) empIdObj;
        return attendanceService.clockOut(employeeId);
    }

    @GetMapping("/abnormal")
    @RequireEmployee
    @Operation(summary = "异常考勤统计", description = "获取本周迟到/早退/缺勤等异常考勤记录")
    @Parameter(name = "weekStart", description = "周起始日期，格式 yyyy-MM-dd", example = "2026-08-17")
    public R<Map<String, Object>> abnormal(@RequestParam(required = false) String weekStart) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> stats = attendanceService.getAbnormalStats(tenantId, weekStart);
        return R.success(stats);
    }
}
