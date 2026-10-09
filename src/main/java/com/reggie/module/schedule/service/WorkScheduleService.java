package com.reggie.module.schedule.service;

import com.reggie.common.R;

import java.util.List;
import java.util.Map;

/**
 * 排班服务接口
 */
public interface WorkScheduleService {

    /**
     * 获取本月排班表
     *
     * @param tenantId 租户ID
     * @param month    月份，格式 yyyy-MM
     * @return 排班表数据
     */
    List<Map<String, Object>> getMonthlySchedule(Long tenantId, String month);

    /**
     * 获取某员工排班
     *
     * @param employeeId 员工ID
     * @param month      月份，格式 yyyy-MM
     * @param tenantId   租户ID
     * @return 员工排班数据
     */
    List<Map<String, Object>> getEmployeeSchedule(Long employeeId, String month, Long tenantId);

    /**
     * 保存/更新排班
     *
     * @param employeeId 员工ID
     * @param date       排班日期，格式 yyyy-MM-dd
     * @param shift      班次（0=早班,1=中班,2=晚班,3=全天）
     * @param shiftStart 班次开始时间
     * @param shiftEnd   班次结束时间
     * @return 操作结果
     */
    R<Void> saveSchedule(Long employeeId, String date, int shift, String shiftStart, String shiftEnd);

    /**
     * 获取今日排班
     *
     * @param tenantId 租户ID
     * @return 今日排班数据
     */
    List<Map<String, Object>> getTodaySchedule(Long tenantId);
}
