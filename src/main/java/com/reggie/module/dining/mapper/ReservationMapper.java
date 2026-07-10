package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预订记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
