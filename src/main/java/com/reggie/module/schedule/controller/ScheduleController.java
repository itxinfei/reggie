package com.reggie.module.schedule.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.schedule.dto.SaveScheduleDTO;
import com.reggie.module.schedule.service.WorkScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 排班管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule")
@Tag(name = "排班管理", description = "员工排班表管理与查询接口")
public class ScheduleController {

    @Autowired
    private WorkScheduleService workScheduleService;

    @GetMapping("/monthly")
    @RequireEmployee
    @Operation(summary = "本月排班表", description = "获取当前租户所有员工本月排班表")
    @Parameter(name = "month", description = "月份，格式 yyyy-MM", example = "2026-08")
    public R<List<Map<String, Object>>> monthly(@RequestParam(required = false) String month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> schedules = workScheduleService.getMonthlySchedule(tenantId, month);
        return R.success(schedules);
    }

    @GetMapping("/employee/{employeeId}")
    @RequireEmployee
    @Operation(summary = "员工排班", description = "获取某员工某月的排班详情")
    @Parameter(name = "employeeId", description = "员工ID", required = true)
    public R<List<Map<String, Object>>> employeeSchedule(@PathVariable Long employeeId,
                                                         @Parameter(description = "月份，格式 yyyy-MM", example = "2026-08") @RequestParam(required = false) String month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> schedules = workScheduleService.getEmployeeSchedule(employeeId, month, tenantId);
        return R.success(schedules);
    }

    @PostMapping("/save")
    @RequireEmployee
    @Operation(summary = "保存排班", description = "保存或更新员工的某日排班信息")
    public R<Void> saveSchedule(@Parameter(description = "排班信息（员工ID、日期、班次、起止时间）", required = true) @Valid @RequestBody SaveScheduleDTO dto) {
        Long employeeId = dto.getEmployeeId();
        String date = dto.getDate();
        int shift = dto.getShift() != null ? dto.getShift() : 0;
        String shiftStart = dto.getShiftStart() != null ? dto.getShiftStart() : "08:00";
        String shiftEnd = dto.getShiftEnd() != null ? dto.getShiftEnd() : "20:00";

        return workScheduleService.saveSchedule(employeeId, date, shift, shiftStart, shiftEnd);
    }

    @GetMapping("/today")
    @RequireEmployee
    @Operation(summary = "今日排班", description = "获取当前租户所有员工的今日排班")
    public R<List<Map<String, Object>>> today() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> schedules = workScheduleService.getTodaySchedule(tenantId);
        return R.success(schedules);
    }
}
