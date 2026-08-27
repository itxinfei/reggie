package com.reggie.module.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.attendance.model.Attendance;
import org.apache.ibatis.annotations.Mapper;

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
}