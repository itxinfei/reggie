package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 预订记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
