package com.reggie.module.schedule.service.impl;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.schedule.model.WorkSchedule;
import com.reggie.module.schedule.service.WorkScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排班服务实现类
 * <p>
 * 注意：目前 work_schedule 表尚未创建，所有方法使用 Mock 数据填充。
 * 后续表创建后可替换为 MyBatis-Plus 查询实现。
 * </p>
 */
@Slf4j
@Service
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 班次映射
     */
    private static final String[] SHIFT_NAMES = {"早班", "中班", "晚班", "全天"};
    private static final String[][] SHIFT_TIME_RANGES = {
            {"07:00", "14:00"},
            {"14:00", "22:00"},
            {"18:00", "02:00"},
            {"08:00", "20:00"}
    };

    @Override
    public List<Map<String, Object>> getMonthlySchedule(Long tenantId, String month) {
        log.info("获取本月排班表 - tenantId={}, month={}", tenantId, month);

        LocalDate monthStart = LocalDate.now();
        if (month != null && !month.isEmpty()) {
            monthStart = LocalDate.parse(month + "-01", DATE_FMT);
        }
        int daysInMonth = monthStart.lengthOfMonth();

        // Mock 员工列表
        String[][] employees = {
                {"1", "张三"},
                {"2", "李四"},
                {"3", "王五"},
                {"4", "赵六"},
                {"5", "孙七"}
        };

        List<Map<String, Object>> result = new ArrayList<>();

        // 按日期生成排班
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDay = monthStart.plusDays(day - 1);
            int dayOfWeek = currentDay.getDayOfWeek().getValue();
            String dateStr = currentDay.format(DATE_FMT);

            for (String[] emp : employees) {
                Map<String, Object> schedule = new HashMap<>();
                schedule.put("employeeId", Long.parseLong(emp[0]));
                schedule.put("employeeName", emp[1]);
                schedule.put("date", dateStr);
                schedule.put("dayOfWeek", dayOfWeek);
                schedule.put("dayOfWeekName", getDayOfWeekName(dayOfWeek));

                if (dayOfWeek == 6 || dayOfWeek == 7) {
                    schedule.put("shift", 3);
                    schedule.put("shiftName", "全天");
                    schedule.put("shiftStart", "08:00");
                    schedule.put("shiftEnd", "20:00");
                    schedule.put("isWorkDay", true);
                } else {
                    // 按员工编号和日期分配班次
                    int shift = (Integer.parseInt(emp[0]) + day) % 4;
                    schedule.put("shift", shift);
                    schedule.put("shiftName", SHIFT_NAMES[shift]);
                    schedule.put("shiftStart", SHIFT_TIME_RANGES[shift][0]);
                    schedule.put("shiftEnd", SHIFT_TIME_RANGES[shift][1]);
                    schedule.put("isWorkDay", true);
                }

                schedule.put("remark", "");
                result.add(schedule);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getEmployeeSchedule(Long employeeId, String month, Long tenantId) {
        log.info("获取员工排班 - employeeId={}, month={}, tenantId={}", employeeId, month, tenantId);

        LocalDate monthStart = LocalDate.now();
        if (month != null && !month.isEmpty()) {
            monthStart = LocalDate.parse(month + "-01", DATE_FMT);
        }
        int daysInMonth = monthStart.lengthOfMonth();

        List<Map<String, Object>> result = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDay = monthStart.plusDays(day - 1);
            int dayOfWeek = currentDay.getDayOfWeek().getValue();
            String dateStr = currentDay.format(DATE_FMT);

            Map<String, Object> schedule = new HashMap<>();
            schedule.put("employeeId", employeeId);
            schedule.put("employeeName", "张三");
            schedule.put("date", dateStr);
            schedule.put("dayOfWeek", dayOfWeek);
            schedule.put("dayOfWeekName", getDayOfWeekName(dayOfWeek));

            int shift = (employeeId.intValue() + day) % 4;
            schedule.put("shift", shift);
            schedule.put("shiftName", SHIFT_NAMES[shift]);
            schedule.put("shiftStart", SHIFT_TIME_RANGES[shift][0]);
            schedule.put("shiftEnd", SHIFT_TIME_RANGES[shift][1]);
            schedule.put("remark", "");

            result.add(schedule);
        }

        return result;
    }

    @Override
    public R<Void> saveSchedule(Long employeeId, String date, int shift, String shiftStart, String shiftEnd) {
        log.info("保存排班 - employeeId={}, date={}, shift={}, shiftStart={}, shiftEnd={}",
                employeeId, date, shift, shiftStart, shiftEnd);

        if (employeeId == null) {
            return R.error("员工ID不能为空");
        }
        if (date == null || date.isEmpty()) {
            return R.error("排班日期不能为空");
        }
        if (shift < 0 || shift > 3) {
            return R.error("班次参数错误，可选范围：0=早班,1=中班,2=晚班,3=全天");
        }

        // 验证时间格式
        try {
            LocalTime.parse(shiftStart);
            LocalTime.parse(shiftEnd);
        } catch (Exception e) {
            return R.error("班次时间格式不正确，请使用 HH:mm 格式");
        }

        // 验证日期格式
        try {
            LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return R.error("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }

        // Mock 保存（实际表创建后用 MyBatis-Plus 实现）
        WorkSchedule schedule = new WorkSchedule();
        schedule.setEmployeeId(employeeId);
        schedule.setScheduleDate(LocalDate.parse(date, DATE_FMT));
        schedule.setShift(shift);
        schedule.setShiftStart(LocalTime.parse(shiftStart));
        schedule.setShiftEnd(LocalTime.parse(shiftEnd));
        schedule.setWorkDateStr(date);
        schedule.setTenantId(BaseContext.getCurrentTenantId());

        R<Void> r = R.success(null);
        r.add("shiftName", SHIFT_NAMES[shift]);
        r.add("date", date);
        return r;
    }

    @Override
    public List<Map<String, Object>> getTodaySchedule(Long tenantId) {
        log.info("获取今日排班 - tenantId={}", tenantId);

        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FMT);

        // Mock 今日排班数据
        String[][] todaySchedules = {
                {"1", "张三", "3", "08:00", "20:00", "全天值班"},
                {"2", "李四", "0", "07:00", "14:00", ""},
                {"3", "王五", "1", "14:00", "22:00", ""},
                {"4", "赵六", "2", "18:00", "02:00", ""},
                {"5", "孙七", "3", "08:00", "20:00", "备班"}
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] row : todaySchedules) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", Long.parseLong(row[0]));
            item.put("employeeName", row[1]);
            item.put("shift", Integer.parseInt(row[2]));
            item.put("shiftName", SHIFT_NAMES[Integer.parseInt(row[2])]);
            item.put("shiftStart", row[3]);
            item.put("shiftEnd", row[4]);
            item.put("remark", row[5]);
            item.put("date", todayStr);
            result.add(item);
        }

        return result;
    }

    private String getDayOfWeekName(int dayOfWeek) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return names[dayOfWeek];
    }
}
