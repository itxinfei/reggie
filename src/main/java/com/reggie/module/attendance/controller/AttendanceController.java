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
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * 处理 week summary。
     * @param weekStart 参数 weekStart
     * @return 返回结果
     */
    @GetMapping("/week-summary")
    @RequireEmployee
    @Operation(summary = "本周考勤汇总", description = "获取本周总出勤人数/缺勤人数/迟到人数/平均工时等汇总数据")
    @Parameter(name = "weekStart", description = "周起始日期，格式 yyyy-MM-dd", example = "2026-08-17")
    public R<Map<String, Object>> weekSummary(@RequestParam(required = false) String weekStart) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> summary = attendanceService.getWeekSummary(tenantId, weekStart);
        return R.success(summary);
    }

    /**
     * 处理 calendar。
     * @param employeeId 参数 employeeId
     * @param month 参数 month
     * @return 返回结果
     */
    @GetMapping("/calendar/{employeeId}")
    @RequireEmployee
    @Operation(summary = "考勤日历", description = "获取某员工某月的每日考勤状态日历")
    @Parameter(name = "employeeId", description = "员工ID", required = true)
    public R<Map<String, Object>> calendar(@PathVariable Long employeeId,
                                           @Parameter(description = "月份，格式 yyyy-MM", example =
                                                   "2026-08") @RequestParam(required = false) String month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> calendar = attendanceService.getAttendanceCalendar(employeeId, month, tenantId);
        return R.success(calendar);
    }

    /**
     * 处理 calendar（查询参数版）。
     * <p>
     * 修改点：前端考勤页以 {@code /api/attendance/calendar?month=yyyy-MM&employeeId=} 调用，
     * 而原接口为路径参数 {@code /calendar/{employeeId}}；页面初始未选择员工时 employeeId 为空，
     * 请求会落到无匹配的路径上，导致 404（页面报错）。此处补充查询参数版本，
     * 未指定或非法 employeeId 时返回空日历，保证页面正常渲染。
     *
     * @param employeeId 员工ID（可为空）
     * @param month      月份，格式 yyyy-MM
     * @return 考勤日历数据；未指定员工时返回空日历
     */
    @GetMapping("/calendar")
    @RequireEmployee
    @Operation(summary = "考勤日历（查询参数）", description = "按员工ID与月份查询考勤日历；未传 employeeId 时返回空日历")
    public R<Map<String, Object>> calendarByQuery(
            @Parameter(description = "员工ID，未选择员工时可为空", example = "900000014001")
            @RequestParam(required = false) String employeeId,
            @Parameter(description = "月份，格式 yyyy-MM", example = "2026-09")
            @RequestParam(required = false) String month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long empId = parseEmployeeId(employeeId);
        if (empId == null) {
            log.info("考勤日历查询：未指定有效员工，返回空日历 - month={}, rawEmployeeId={}", month, employeeId);
            return R.success(buildEmptyCalendar(month));
        }
        Map<String, Object> calendar = attendanceService.getAttendanceCalendar(empId, month, tenantId);
        return R.success(calendar);
    }

    /**
     * 解析员工ID：空值或非数字返回 null，避免非法入参导致 500。
     *
     * @param employeeId 员工ID字符串
     * @return 员工ID；为空或非法时返回 null
     */
    private Long parseEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(employeeId.trim());
        } catch (NumberFormatException e) {
            log.warn("考勤日历查询：员工ID非法 - employeeId={}", employeeId);
            return null;
        }
    }

    /**
     * 构建空日历，作为未选择员工时的兜底数据。
     *
     * @param month 月份，格式 yyyy-MM
     * @return 空日历数据（calendar 为空数组）
     */
    private Map<String, Object> buildEmptyCalendar(String month) {
        Map<String, Object> summary = new HashMap<>(5);
        summary.put("presentDays", 0);
        summary.put("lateDays", 0);
        summary.put("leaveDays", 0);
        summary.put("absentDays", 0);
        summary.put("totalWorkHours", 0);

        Map<String, Object> result = new HashMap<>(4);
        result.put("employeeId", null);
        result.put("month", month);
        result.put("calendar", Collections.emptyList());
        result.put("summary", summary);
        return result;
    }

    /**
     * 处理 today。
     * @return 返回结果
     */
    @GetMapping("/today")
    @RequireEmployee
    @Operation(summary = "今日考勤", description = "获取今日已到岗/未到岗/请假员工列表及汇总")
    public R<Map<String, Object>> today() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> today = attendanceService.getTodayAttendance(tenantId);
        return R.success(today);
    }

    /**
     * 处理 clock in。
     * @param request 参数 request
     * @return 返回结果
     */
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

    /**
     * 处理 clock out。
     * @param request 参数 request
     * @return 返回结果
     */
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

    /**
     * 处理 abnormal。
     * @param weekStart 参数 weekStart
     * @return 返回结果
     */
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
