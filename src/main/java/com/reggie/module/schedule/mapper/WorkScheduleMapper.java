package com.reggie.module.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.schedule.model.WorkSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 排班记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-08-26
 */
@Mapper
public interface WorkScheduleMapper extends BaseMapper<WorkSchedule> {
}