package com.reggie.module.attendance.service.impl;

import com.reggie.common.R;
import com.reggie.module.attendance.service.AttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤服务实现类
 * <p>
 * 注意：目前 attendance 表尚未创建，所有方法使用 Mock 数据填充。
 * 后续表创建后可替换为 MyBatis-Plus 查询实现。
 * </p>
 */
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    /** 日期格式化 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 获取本周考勤汇总
     *
     * @param tenantId  租户ID
     * @param weekStart 周起始日期
     * @return 汇总数据
     */
    @Override
    public Map<String, Object> getWeekSummary(Long tenantId, String weekStart) {
        log.info("获取本周考勤汇总 - tenantId={}, weekStart={}", tenantId, weekStart);

        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("weekStart", weekStart);

        LocalDate startDate = LocalDate.now();
        if (weekStart != null && !weekStart.isEmpty()) {
            startDate = LocalDate.parse(weekStart, DATE_FMT);
        }
        LocalDate endDate = startDate.plusDays(6);
        result.put("weekEnd", endDate.format(DATE_FMT));

        // Mock 汇总数据
        result.put("totalEmployees", 12);
        result.put("presentCount", 9);
        result.put("absentCount", 1);
        result.put("lateCount", 2);
        result.put("earlyLeaveCount", 1);
        result.put("leaveCount", 3);
        result.put("businessTripCount", 1);
        result.put("averageWorkHours", new BigDecimal("7.6"));

        return result;
    }

    /**
     * 获取员工考勤日历
     *
     * @param employeeId 员工ID
     * @param month      月份
     * @param tenantId   租户ID
     * @return 考勤日历数据
     */
    @Override
    public Map<String, Object> getAttendanceCalendar(Long employeeId, String month, Long tenantId) {
        log.info("获取员工考勤日历 - employeeId={}, month={}, tenantId={}", employeeId, month, tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("month", month);
        result.put("employeeName", "张三");

        LocalDate monthStart = LocalDate.now();
        if (month != null && !month.isEmpty()) {
            monthStart = LocalDate.parse(month + "-01", DATE_FMT);
        }
        int daysInMonth = monthStart.lengthOfMonth();

        List<Map<String, Object>> calendar = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDay = monthStart.plusDays(day - 1);
            Map<String, Object> dayRecord = new HashMap<>();
            dayRecord.put("date", currentDay.format(DATE_FMT));
            dayRecord.put("day", day);

            int dayOfWeek = currentDay.getDayOfWeek().getValue();
            // 周末为缺勤
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                dayRecord.put("status", 0);
                dayRecord.put("statusName", "缺勤");
                dayRecord.put("checkInTime", null);
                dayRecord.put("checkOutTime", null);
                dayRecord.put("workHours", BigDecimal.ZERO);
            } else {
                // 工作日 Mock 数据，随机迟到/正常
                int mockStatus;
                int remainder = (int) (day % 5);
                if (remainder == 0) {
                    mockStatus = 2;
                } else if (remainder == 1) {
                    mockStatus = 4;
                } else {
                    mockStatus = 1;
                }

                String statusName;
                BigDecimal workHours = BigDecimal.ZERO;
                LocalDateTime checkIn = null;
                LocalDateTime checkOut = null;

                if (mockStatus == 1) {
                    statusName = "正常";
                    checkIn = currentDay.atTime(8, 30);
                    checkOut = currentDay.atTime(18, 0);
                    workHours = new BigDecimal("9.0");
                } else if (mockStatus == 2) {
                    statusName = "迟到";
                    checkIn = currentDay.atTime(8, 45);
                    checkOut = currentDay.atTime(18, 0);
                    workHours = new BigDecimal("9.0");
                } else if (mockStatus == 4) {
                    statusName = "请假";
                    workHours = BigDecimal.ZERO;
                } else {
                    statusName = "缺勤";
                    workHours = BigDecimal.ZERO;
                }

                dayRecord.put("status", mockStatus);
                dayRecord.put("statusName", statusName);
                dayRecord.put("checkInTime", checkIn != null ? checkIn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
                dayRecord.put("checkOutTime", checkOut != null ? checkOut.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
                dayRecord.put("workHours", workHours);
            }

            calendar.add(dayRecord);
        }

        result.put("calendar", calendar);

        // 本月汇总
        int presentDays = 0;
        int lateDays = 0;
        int leaveDays = 0;
        int absentDays = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        for (Map<String, Object> record : calendar) {
            Integer status = (Integer) record.get("status");
            BigDecimal hours = (BigDecimal) record.get("workHours");
            if (status == 1) {
                presentDays++;
                totalHours = totalHours.add(hours != null ? hours : BigDecimal.ZERO);
            } else if (status == 2) {
                lateDays++;
                totalHours = totalHours.add(hours != null ? hours : BigDecimal.ZERO);
            } else if (status == 4) {
                leaveDays++;
            } else if (status == 0) {
                absentDays++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("presentDays", presentDays);
        summary.put("lateDays", lateDays);
        summary.put("leaveDays", leaveDays);
        summary.put("absentDays", absentDays);
        summary.put("totalWorkHours", totalHours.setScale(2, RoundingMode.HALF_UP));
        result.put("summary", summary);

        return result;
    }

    /**
     * 获取今日考勤
     *
     * @param tenantId 租户ID
     * @return 今日考勤数据
     */
    @Override
    public Map<String, Object> getTodayAttendance(Long tenantId) {
        log.info("获取今日考勤 - tenantId={}", tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("date", LocalDate.now().format(DATE_FMT));

        // Mock 已到岗员工列表
        List<Map<String, Object>> checkedInList = new ArrayList<>();
        String[][] checkedInData = {
                {"1", "张三", "08:30:00"},
                {"2", "李四", "08:35:00"},
                {"3", "王五", "08:45:00"}
        };
        for (String[] row : checkedInData) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", Long.parseLong(row[0]));
            item.put("employeeName", row[1]);
            item.put("checkInTime", LocalDate.now() + " " + row[2]);
            checkedInList.add(item);
        }
        result.put("checkedInList", checkedInList);

        // Mock 未到岗员工列表
        List<Map<String, Object>> notCheckedInList = new ArrayList<>();
        String[][] notCheckedData = {
                {"4", "赵六"},
                {"5", "孙七"},
                {"6", "周八"}
        };
        for (String[] row : notCheckedData) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", Long.parseLong(row[0]));
            item.put("employeeName", row[1]);
            item.put("checkInTime", null);
            notCheckedInList.add(item);
        }
        result.put("notCheckedInList", notCheckedInList);

        // Mock 请假列表
        List<Map<String, Object>> leaveList = new ArrayList<>();
        Map<String, Object> leaveItem = new HashMap<>();
        leaveItem.put("employeeId", 7);
        leaveItem.put("employeeName", "吴九");
        leaveItem.put("reason", "事假");
        leaveList.add(leaveItem);
        result.put("leaveList", leaveList);

        // 汇总
        result.put("checkedInCount", checkedInList.size());
        result.put("notCheckedInCount", notCheckedInList.size());
        result.put("leaveCount", leaveList.size());
        result.put("totalEmployees", checkedInList.size() + notCheckedInList.size() + leaveList.size());

        return result;
    }

    /**
     * 打卡签到
     *
     * @param employeeId 员工ID
     * @return 操作结果
     */
    @Override
    public R<Void> clockIn(Long employeeId) {
        log.info("员工打卡签到 - employeeId={}", employeeId);

        if (employeeId == null) {
            return R.error("员工ID不能为空");
        }

        // 模拟签到逻辑
        LocalDateTime now = LocalDateTime.now();
        LocalTime current = now.toLocalTime();
        LocalTime lateThreshold = LocalTime.of(8, 30);

        String status;
        if (current.isAfter(lateThreshold)) {
            status = "签到成功（迟到）";
        } else {
            status = "签到成功";
        }

        log.info("签到完成 - employeeId={}, time={}, status={}", employeeId, now, status);

        R<Void> r = R.success(null);
        r.add("status", status);
        r.add("checkInTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return r;
    }

    /**
     * 打卡签退
     *
     * @param employeeId 员工ID
     * @return 操作结果
     */
    @Override
    public R<Void> clockOut(Long employeeId) {
        log.info("员工打卡签退 - employeeId={}", employeeId);

        if (employeeId == null) {
            return R.error("员工ID不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime current = now.toLocalTime();
        LocalTime earlyLeaveThreshold = LocalTime.of(17, 30);

        String status;
        if (current.isBefore(earlyLeaveThreshold)) {
            status = "签退成功（早退）";
        } else {
            status = "签退成功";
        }

        log.info("签退完成 - employeeId={}, time={}, status={}", employeeId, now, status);

        R<Void> r = R.success(null);
        r.add("status", status);
        r.add("checkOutTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return r;
    }

    /**
     * 统计异常考勤
     *
     * @param tenantId  租户ID
     * @param weekStart 周起始日期
     * @return 异常统计数据
     */
    @Override
    public Map<String, Object> getAbnormalStats(Long tenantId, String weekStart) {
        log.info("统计异常考勤 - tenantId={}, weekStart={}", tenantId, weekStart);

        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("weekStart", weekStart != null ? weekStart : LocalDate.now().format(DATE_FMT));

        // Mock 异常考勤详情
        List<Map<String, Object>> abnormalList = new ArrayList<>();

        String[][] abnormalData = {
                {"1", "张三", "2", "迟到", "2026-08-17", "08:45"},
                {"1", "张三", "2", "迟到", "2026-08-19", "08:50"},
                {"3", "王五", "2", "迟到", "2026-08-18", "09:00"},
                {"4", "赵六", "0", "缺勤", "2026-08-20", null},
                {"5", "孙七", "3", "早退", "2026-08-17", "17:20"}
        };

        for (String[] row : abnormalData) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", Long.parseLong(row[0]));
            item.put("employeeName", row[1]);
            item.put("status", Integer.parseInt(row[2]));
            item.put("statusName", row[3]);
            item.put("date", row[4]);
            item.put("time", row[5]);
            abnormalList.add(item);
        }

        result.put("abnormalList", abnormalList);
        result.put("lateCount", 3);
        result.put("earlyLeaveCount", 1);
        result.put("absentCount", 1);
        result.put("totalAbnormalCount", 5);

        return result;
    }
}
