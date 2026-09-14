package com.reggie.module.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.schedule.mapper.WorkScheduleMapper;
import com.reggie.module.schedule.model.WorkSchedule;
import com.reggie.module.schedule.service.WorkScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排班表 业务实现层
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Slf4j
@Service
public class WorkScheduleServiceImpl extends ServiceImpl<WorkScheduleMapper, WorkSchedule>
        implements WorkScheduleService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private WorkScheduleMapper workScheduleMapper;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 查询当月排班：按员工+日期维度汇总，每人每天一行
     *
     * @param tenantId 租户ID
     * @param month    格式 yyyy-MM，默认当月
     * @return 排班列表（每行含 employeeId/employeeName/date/shift/shiftStart/shiftEnd/remark）
     */
    @Override
    public List<Map<String, Object>> getMonthlySchedule(Long tenantId, String month) {
        // 解析月份范围
        YearMonth ym = parseMonth(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 查询该月所有排班记录
        LambdaQueryWrapper<WorkSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkSchedule::getTenantId, tenantId)
                .ge(WorkSchedule::getScheduleDate, startDate)
                .le(WorkSchedule::getScheduleDate, endDate)
                .orderByAsc(WorkSchedule::getScheduleDate)
                .orderByAsc(WorkSchedule::getEmployeeId);
        List<WorkSchedule> records = workScheduleMapper.selectList(wrapper);

        // 转换为前端需要的 Map 结构
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkSchedule ws : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ws.getId());
            item.put("employeeId", ws.getEmployeeId());
            item.put("employeeName", ws.getEmployeeName());
            item.put("date", ws.getWorkDateStr() != null ? ws.getWorkDateStr()
                    : ws.getScheduleDate().format(DATE_FMT));
            item.put("shift", ws.getShift());
            item.put("shiftStart", ws.getShiftStart() != null ? ws.getShiftStart().toString() : "08:00");
            item.put("shiftEnd", ws.getShiftEnd() != null ? ws.getShiftEnd().toString() : "18:00");
            item.put("remark", ws.getRemark());
            result.add(item);
        }

        return result;
    }

    /**
     * 查询某员工某月排班
     *
     * @param employeeId 员工ID
     * @param month      格式 yyyy-MM，默认当月
     * @param tenantId   租户ID
     * @return 排班列表
     */
    @Override
    public List<Map<String, Object>> getEmployeeSchedule(Long employeeId, String month, Long tenantId) {
        YearMonth ym = parseMonth(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        LambdaQueryWrapper<WorkSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkSchedule::getTenantId, tenantId)
                .eq(WorkSchedule::getEmployeeId, employeeId)
                .ge(WorkSchedule::getScheduleDate, startDate)
                .le(WorkSchedule::getScheduleDate, endDate)
                .orderByAsc(WorkSchedule::getScheduleDate);
        List<WorkSchedule> records = workScheduleMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkSchedule ws : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ws.getId());
            item.put("employeeId", ws.getEmployeeId());
            item.put("employeeName", ws.getEmployeeName());
            item.put("date", ws.getWorkDateStr() != null ? ws.getWorkDateStr()
                    : ws.getScheduleDate().format(DATE_FMT));
            item.put("shift", ws.getShift());
            item.put("shiftStart", ws.getShiftStart() != null ? ws.getShiftStart().toString() : "08:00");
            item.put("shiftEnd", ws.getShiftEnd() != null ? ws.getShiftEnd().toString() : "18:00");
            item.put("remark", ws.getRemark());
            result.add(item);
        }

        return result;
    }

    /**
     * 保存排班：新增或更新某员工某日排班
     *
     * @param employeeId 员工ID
     * @param date       日期，格式 yyyy-MM-dd
     * @param shift      班次（0=早班, 1=中班, 2=晚班, 3=全天）
     * @param shiftStart 班次开始时间
     * @param shiftEnd   班次结束时间
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> saveSchedule(Long employeeId, String date, int shift, String shiftStart, String shiftEnd) {
        Long tenantId = BaseContext.getCurrentTenantId();

        // 校验员工存在且属于当前租户
        Employee employee = employeeService.getById(employeeId);
        if (employee == null || !tenantId.equals(employee.getTenantId())) {
            throw new CustomException("员工不存在");
        }

        LocalDate scheduleDate = LocalDate.parse(date, DATE_FMT);

        // 检查是否已有该日排班（同租户+同员工+同日期）
        LambdaQueryWrapper<WorkSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkSchedule::getTenantId, tenantId)
                .eq(WorkSchedule::getEmployeeId, employeeId)
                .eq(WorkSchedule::getScheduleDate, scheduleDate);
        WorkSchedule existing = workScheduleMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新已有排班
            existing.setShift(shift);
            existing.setShiftStart(LocalTime.parse(ensureTimeFormat(shiftStart)));
            existing.setShiftEnd(LocalTime.parse(ensureTimeFormat(shiftEnd)));
            workScheduleMapper.updateById(existing);
        } else {
            // 新增排班
            WorkSchedule ws = new WorkSchedule();
            ws.setEmployeeId(employeeId);
            ws.setEmployeeName(employee.getName());
            ws.setScheduleDate(scheduleDate);
            ws.setShift(shift);
            ws.setShiftStart(LocalTime.parse(ensureTimeFormat(shiftStart)));
            ws.setShiftEnd(LocalTime.parse(ensureTimeFormat(shiftEnd)));
            ws.setWorkDateStr(date);
            ws.setTenantId(tenantId);
            workScheduleMapper.insert(ws);
        }

        return R.success(null);
    }

    /**
     * 查询今日排班
     *
     * @param tenantId 租户ID
     * @return 今日排班列表
     */
    @Override
    public List<Map<String, Object>> getTodaySchedule(Long tenantId) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<WorkSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkSchedule::getTenantId, tenantId)
                .eq(WorkSchedule::getScheduleDate, today)
                .orderByAsc(WorkSchedule::getShiftStart);
        List<WorkSchedule> records = workScheduleMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkSchedule ws : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ws.getId());
            item.put("employeeId", ws.getEmployeeId());
            item.put("employeeName", ws.getEmployeeName());
            item.put("shift", ws.getShift());
            item.put("shiftStart", ws.getShiftStart() != null ? ws.getShiftStart().toString() : "08:00");
            item.put("shiftEnd", ws.getShiftEnd() != null ? ws.getShiftEnd().toString() : "18:00");
            item.put("remark", ws.getRemark());
            result.add(item);
        }

        return result;
    }

    /**
     * 解析月份字符串，默认当月
     */
    private YearMonth parseMonth(String month) {
        if (month != null && !month.isEmpty()) {
            return YearMonth.parse(month, MONTH_FMT);
        }
        return YearMonth.now();
    }

    /**
     * 确保时间为 HH:mm 或 HH:mm:ss 格式，不足补 :00
     */
    private String ensureTimeFormat(String time) {
        if (time == null || time.isEmpty()) {
            return "08:00";
        }
        // 如果只有 HH:mm，补 :00 变为 HH:mm:ss
        if (time.length() == 5) {
            return time + ":00";
        }
        return time;
    }
}
