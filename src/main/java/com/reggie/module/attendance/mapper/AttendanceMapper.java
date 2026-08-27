package com.reggie.module.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.attendance.model.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 考勤记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-08-26
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    /**
     * 按周汇总考勤状态计数
     *
     * @param tenantId  租户ID
     * @param weekStart 周起始日期
     * @param weekEnd   周结束日期
     * @return 各状态计数 Map: status -> count
     */
    List<Map<String, Object>> countByStatusInWeek(@Param("tenantId") Long tenantId,
                                                    @Param("weekStart") LocalDate weekStart,
                                                    @Param("weekEnd") LocalDate weekEnd);

    /**
     * 按周计算平均工时
     */
    BigDecimal avgWorkHoursInWeek(@Param("tenantId") Long tenantId,
                                   @Param("weekStart") LocalDate weekStart,
                                   @Param("weekEnd") LocalDate weekEnd);

    /**
     * 按周统计有考勤记录的员工总数
     */
    Integer countDistinctEmployeesInWeek(@Param("tenantId") Long tenantId,
                                          @Param("weekStart") LocalDate weekStart,
                                          @Param("weekEnd") LocalDate weekEnd);

    /**
     * 获取员工某月的每日考勤记录（去重，每天保留最新一条）
     */
    List<Attendance> listByEmployeeAndMonth(@Param("employeeId") Long employeeId,
                                              @Param("tenantId") Long tenantId,
                                              @Param("monthStart") LocalDate monthStart,
                                              @Param("monthEnd") LocalDate monthEnd);

    /**
     * 获取今日考勤记录（按租户过滤）
     */
    List<Attendance> listByDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /**
     * 获取异常考勤记录（缺勤/迟到/早退）
     */
    List<Attendance> listAbnormalInWeek(@Param("tenantId") Long tenantId,
                                         @Param("weekStart") LocalDate weekStart,
                                         @Param("weekEnd") LocalDate weekEnd);
}