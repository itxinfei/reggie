package com.reggie.module.attendance.service;

import com.reggie.common.R;

import java.util.Map;

/**
 * 考勤服务接口
 */
public interface AttendanceService {

    /**
     * 获取本周考勤汇总（总出勤人数/缺勤人数/迟到人数/平均工时）
     *
     * @param tenantId  租户ID
     * @param weekStart 周起始日期，格式 yyyy-MM-dd
     * @return 汇总数据
     */
    Map<String, Object> getWeekSummary(Long tenantId, String weekStart);

    /**
     * 获取员工考勤日历（某员工某月的每日考勤状态）
     *
     * @param employeeId 员工ID
     * @param month      月份，格式 yyyy-MM
     * @param tenantId   租户ID
     * @return 考勤日历数据
     */
    Map<String, Object> getAttendanceCalendar(Long employeeId, String month, Long tenantId);

    /**
     * 获取今日考勤（已到岗/未到岗/请假列表）
     *
     * @param tenantId 租户ID
     * @return 今日考勤数据
     */
    Map<String, Object> getTodayAttendance(Long tenantId);

    /**
     * 打卡签到
     *
     * @param employeeId 员工ID
     * @return 操作结果
     */
    R<Void> clockIn(Long employeeId);

    /**
     * 打卡签退
     *
     * @param employeeId 员工ID
     * @return 操作结果
     */
    R<Void> clockOut(Long employeeId);

    /**
     * 统计异常考勤
     *
     * @param tenantId  租户ID
     * @param weekStart 周起始日期，格式 yyyy-MM-dd
     * @return 异常统计数据
     */
    Map<String, Object> getAbnormalStats(Long tenantId, String weekStart);
}
