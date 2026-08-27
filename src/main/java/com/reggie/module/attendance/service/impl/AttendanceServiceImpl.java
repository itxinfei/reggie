package com.reggie.module.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.module.attendance.dto.AbnormalStatsVO;
import com.reggie.module.attendance.dto.AttendanceCalendarVO;
import com.reggie.module.attendance.dto.TodayAttendanceVO;
import com.reggie.module.attendance.dto.WeekSummaryVO;
import com.reggie.module.attendance.mapper.AttendanceMapper;
import com.reggie.module.attendance.model.Attendance;
import com.reggie.module.attendance.service.AttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.stream.Collectors;

/**
 * 考勤服务实现类
 * 基于 attendance 表进行真实数据库操作
 */
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    /** 签到阈值（8:30，超过则判为迟到） */
    private static final LocalTime CHECK_IN_LATE_THRESHOLD = LocalTime.of(8, 30);

    /** 签退阈值（17:30，早于此则判为早退） */
    private static final LocalTime CHECK_OUT_EARLY_THRESHOLD = LocalTime.of(17, 30);

    /** 日期格式化 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 考勤状态描述映射 */
    private static final Map<Integer, String> STATUS_DESC = new HashMap<>();

    static {
        STATUS_DESC.put(0, "缺勤");
        STATUS_DESC.put(1, "正常");
        STATUS_DESC.put(2, "迟到");
        STATUS_DESC.put(3, "早退");
        STATUS_DESC.put(4, "请假");
        STATUS_DESC.put(5, "出差");
    }

    @Autowired
    private AttendanceMapper attendanceMapper;

    // ==================== 本周汇总 ====================

    @Override
    public Map<String, Object> getWeekSummary(Long tenantId, String weekStart) {
        log.info("获取本周考勤汇总 - tenantId={}, weekStart={}", tenantId, weekStart);

        LocalDate startDate = LocalDate.now();
        if (weekStart != null && !weekStart.isEmpty()) {
            startDate = LocalDate.parse(weekStart, DATE_FMT);
        }
        LocalDate endDate = startDate.plusDays(6);

        WeekSummaryVO vo = new WeekSummaryVO();
        vo.setWeekStart(startDate.format(DATE_FMT));
        vo.setWeekEnd(endDate.format(DATE_FMT));

        // 从 DB 查询各状态计数
        List<Map<String, Object>> statusCounts = attendanceMapper.countByStatusInWeek(tenantId, startDate, endDate);
        Map<Integer, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : statusCounts) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status != null && cnt != null) {
                int s = ((Number) status).intValue();
                long c = ((Number) cnt).longValue();
                countMap.put(s, c);
            }
        }

        int normalCount = toInt(countMap.get(1));
        int lateCount = toInt(countMap.get(2));
        int earlyLeaveCount = toInt(countMap.get(3));
        int absentCount = toInt(countMap.get(0));
        int leaveCount = toInt(countMap.get(4));
        int businessTripCount = toInt(countMap.get(5));
        int totalAttendance = normalCount + lateCount + earlyLeaveCount + absentCount + leaveCount + businessTripCount;

        Integer employeeTotal = attendanceMapper.countDistinctEmployeesInWeek(tenantId, startDate, endDate);
        BigDecimal avgWorkHours = attendanceMapper.avgWorkHoursInWeek(tenantId, startDate, endDate);

        vo.setTotalAttendance(totalAttendance);
        vo.setNormalCount(normalCount);
        vo.setLateCount(lateCount);
        vo.setEarlyLeaveCount(earlyLeaveCount);
        vo.setAbsentCount(absentCount);
        vo.setLeaveCount(leaveCount);
        vo.setBusinessTripCount(businessTripCount);
        vo.setEmployeeTotal(employeeTotal);
        vo.setAvgWorkHours(avgWorkHours != null ? avgWorkHours.setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

        if (employeeTotal != null && employeeTotal > 0) {
            double rate = (double) (normalCount + lateCount + earlyLeaveCount + businessTripCount) / employeeTotal * 100;
            vo.setAttendanceRate(Math.round(rate * 100.0) / 100.0);
        } else {
            vo.setAttendanceRate(0.0);
        }

        // 保持向后兼容的 Map 结构
        Map<String, Object> result = new HashMap<>();
        result.put("weekStart", vo.getWeekStart());
        result.put("weekEnd", vo.getWeekEnd());
        result.put("totalEmployees", employeeTotal);
        result.put("totalAttendance", totalAttendance);
        result.put("presentCount", normalCount);
        result.put("lateCount", lateCount);
        result.put("earlyLeaveCount", earlyLeaveCount);
        result.put("absentCount", absentCount);
        result.put("leaveCount", leaveCount);
        result.put("businessTripCount", businessTripCount);
        result.put("averageWorkHours", vo.getAvgWorkHours());
        result.put("attendanceRate", vo.getAttendanceRate());
        return result;
    }

    // ==================== 考勤日历 ====================

    @Override
    public Map<String, Object> getAttendanceCalendar(Long employeeId, String month, Long tenantId) {
        log.info("获取员工考勤日历 - employeeId={}, month={}, tenantId={}", employeeId, month, tenantId);

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        if (month != null && !month.isEmpty()) {
            monthStart = LocalDate.parse(month + "-01", DATE_FMT);
        }
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // 从 DB 查询该员工当月考勤记录
        List<Attendance> attendanceList = attendanceMapper.listByEmployeeAndMonth(
                employeeId, tenantId, monthStart, monthEnd);

        // 构建以日期为 key 的查找表
        Map<LocalDate, Attendance> recordByDate = attendanceList.stream()
                .filter(a -> a.getDate() != null)
                .collect(Collectors.toMap(Attendance::getDate, a -> a, (a1, a2) -> a1));

        List<Map<String, Object>> calendar = new ArrayList<>();
        int presentDays = 0, lateDays = 0, leaveDays = 0, absentDays = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        for (int day = 1; day <= monthStart.lengthOfMonth(); day++) {
            LocalDate currentDay = monthStart.plusDays(day - 1);
            Map<String, Object> dayRecord = new HashMap<>();
            dayRecord.put("date", currentDay.format(DATE_FMT));
            dayRecord.put("day", day);
            int dayOfWeek = currentDay.getDayOfWeek().getValue();

            Attendance record = recordByDate.get(currentDay);
            if (record != null) {
                Integer status = record.getStatus();
                dayRecord.put("status", status);
                dayRecord.put("statusName", STATUS_DESC.getOrDefault(status, "未知"));
                dayRecord.put("checkInTime",
                        record.getCheckInTime() != null ? record.getCheckInTime().format(DT_FMT) : null);
                dayRecord.put("checkOutTime",
                        record.getCheckOutTime() != null ? record.getCheckOutTime().format(DT_FMT) : null);
                BigDecimal workHours = record.getWorkHours() != null ? record.getWorkHours() : BigDecimal.ZERO;
                dayRecord.put("workHours", workHours);

                // 统计汇总
                if (status == 1 || status == 2) {
                    if (status == 1) presentDays++; else lateDays++;
                    totalHours = totalHours.add(workHours);
                } else if (status == 4) {
                    leaveDays++;
                } else if (status == 0) {
                    absentDays++;
                }
            } else {
                // 无记录的日期：周末视为缺勤，工作日视为无数据
                if (dayOfWeek == 6 || dayOfWeek == 7) {
                    dayRecord.put("status", 0);
                    dayRecord.put("statusName", "缺勤");
                    absentDays++;
                } else {
                    dayRecord.put("status", null);
                    dayRecord.put("statusName", "无记录");
                }
                dayRecord.put("checkInTime", null);
                dayRecord.put("checkOutTime", null);
                dayRecord.put("workHours", BigDecimal.ZERO);
            }

            calendar.add(dayRecord);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("presentDays", presentDays);
        summary.put("lateDays", lateDays);
        summary.put("leaveDays", leaveDays);
        summary.put("absentDays", absentDays);
        summary.put("totalWorkHours", totalHours.setScale(2, RoundingMode.HALF_UP));

        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("month", monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        result.put("calendar", calendar);
        result.put("summary", summary);
        return result;
    }

    // ==================== 今日考勤 ====================

    @Override
    public Map<String, Object> getTodayAttendance(Long tenantId) {
        log.info("获取今日考勤 - tenantId={}", tenantId);

        LocalDate today = LocalDate.now();
        List<Attendance> todayList = attendanceMapper.listByDate(tenantId, today);

        List<Map<String, Object>> checkedInList = new ArrayList<>();
        List<Map<String, Object>> notCheckedInList = new ArrayList<>();
        List<Map<String, Object>> leaveList = new ArrayList<>();

        for (Attendance a : todayList) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", a.getEmployeeId());
            item.put("employeeName", a.getEmployeeName());
            item.put("checkInTime",
                    a.getCheckInTime() != null ? a.getCheckInTime().format(DT_FMT) : null);
            item.put("status", a.getStatus());

            Integer status = a.getStatus();
            if (status == 4) {
                leaveList.add(item);
            } else if (a.getCheckInTime() != null) {
                checkedInList.add(item);
            } else {
                notCheckedInList.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", today.format(DATE_FMT));
        result.put("checkedInList", checkedInList);
        result.put("notCheckedInList", notCheckedInList);
        result.put("leaveList", leaveList);
        result.put("checkedInCount", checkedInList.size());
        result.put("notCheckedInCount", notCheckedInList.size());
        result.put("leaveCount", leaveList.size());
        result.put("totalEmployees", checkedInList.size() + notCheckedInList.size() + leaveList.size());
        return result;
    }

    // ==================== 打卡 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> clockIn(Long employeeId) {
        log.info("员工打卡签到 - employeeId={}", employeeId);

        if (employeeId == null) {
            return R.error("员工ID不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 检查今日是否已有签到记录
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getEmployeeId, employeeId)
                .eq(Attendance::getDate, today)
                .last("LIMIT 1");
        Attendance existing = attendanceMapper.selectOne(wrapper);

        int status;
        String statusMsg;
        if (now.toLocalTime().isAfter(CHECK_IN_LATE_THRESHOLD)) {
            status = 2; // 迟到
            statusMsg = "签到成功（迟到）";
        } else {
            status = 1; // 正常
            statusMsg = "签到成功";
        }

        if (existing != null) {
            // 更新已有记录
            existing.setCheckInTime(now);
            existing.setStatus(status);
            existing.setUpdateTime(LocalDateTime.now());
            attendanceMapper.updateById(existing);
        } else {
            // 新建记录
            Attendance record = new Attendance();
            record.setEmployeeId(employeeId);
            record.setDate(today);
            record.setCheckInTime(now);
            record.setStatus(status);
            attendanceMapper.insert(record);
        }

        log.info("签到完成 - employeeId={}, time={}, status={}", employeeId, now, statusMsg);

        R<Void> r = R.success(null);
        r.add("status", statusMsg);
        r.add("checkInTime", now.format(DT_FMT));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> clockOut(Long employeeId) {
        log.info("员工打卡签退 - employeeId={}", employeeId);

        if (employeeId == null) {
            return R.error("员工ID不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getEmployeeId, employeeId)
                .eq(Attendance::getDate, today)
                .last("LIMIT 1");
        Attendance existing = attendanceMapper.selectOne(wrapper);

        if (existing == null) {
            return R.error("今日尚未签到，无法签退");
        }

        // 计算工时
        LocalDateTime checkInTime = existing.getCheckInTime();
        BigDecimal workHours = BigDecimal.ZERO;
        if (checkInTime != null) {
            long minutes = ChronoUnit.MINUTES.between(checkInTime, now);
            workHours = new BigDecimal(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        }

        // 判断早退：更新状态为早退（仅当原本为正常状态）
        if (now.toLocalTime().isBefore(CHECK_OUT_EARLY_THRESHOLD) && existing.getStatus() == 1) {
            existing.setStatus(3); // 早退
        }

        existing.setCheckOutTime(now);
        existing.setWorkHours(workHours);
        existing.setUpdateTime(LocalDateTime.now());
        attendanceMapper.updateById(existing);

        String statusMsg = now.toLocalTime().isBefore(CHECK_OUT_EARLY_THRESHOLD) ? "签退成功（早退）" : "签退成功";
        log.info("签退完成 - employeeId={}, time={}, workHours={}, status={}", employeeId, now, workHours, statusMsg);

        R<Void> r = R.success(null);
        r.add("status", statusMsg);
        r.add("checkOutTime", now.format(DT_FMT));
        r.add("workHours", workHours);
        return r;
    }

    // ==================== 异常统计 ====================

    @Override
    public Map<String, Object> getAbnormalStats(Long tenantId, String weekStart) {
        log.info("统计异常考勤 - tenantId={}, weekStart={}", tenantId, weekStart);

        LocalDate startDate = LocalDate.now();
        if (weekStart != null && !weekStart.isEmpty()) {
            startDate = LocalDate.parse(weekStart, DATE_FMT);
        }
        LocalDate endDate = startDate.plusDays(6);

        List<Attendance> abnormalList = attendanceMapper.listAbnormalInWeek(tenantId, startDate, endDate);

        List<Map<String, Object>> resultList = new ArrayList<>();
        int lateCount = 0, earlyLeaveCount = 0, absentCount = 0;

        for (Attendance a : abnormalList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("employeeId", a.getEmployeeId());
            item.put("employeeName", a.getEmployeeName());
            item.put("status", a.getStatus());
            item.put("statusName", STATUS_DESC.getOrDefault(a.getStatus(), "未知"));
            item.put("date", a.getDate() != null ? a.getDate().format(DATE_FMT) : null);
            item.put("checkInTime",
                    a.getCheckInTime() != null ? a.getCheckInTime().format(DT_FMT) : null);
            item.put("checkOutTime",
                    a.getCheckOutTime() != null ? a.getCheckOutTime().format(DT_FMT) : null);
            item.put("workHours", a.getWorkHours());
            resultList.add(item);

            if (a.getStatus() == 2) lateCount++;
            else if (a.getStatus() == 3) earlyLeaveCount++;
            else if (a.getStatus() == 0) absentCount++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("weekStart", startDate.format(DATE_FMT));
        result.put("weekEnd", endDate.format(DATE_FMT));
        result.put("abnormalList", resultList);
        result.put("lateCount", lateCount);
        result.put("earlyLeaveCount", earlyLeaveCount);
        result.put("absentCount", absentCount);
        result.put("totalAbnormalCount", resultList.size());
        return result;
    }

    // ==================== 工具方法 ====================

    private int toInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
}