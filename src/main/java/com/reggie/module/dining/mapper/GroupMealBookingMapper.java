package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.GroupMealBooking;
import org.apache.ibatis.annotations.Mapper;

/** 团餐预订 Mapper（简单 CRUD 走 BaseMapper）。 */
@Mapper
public interface GroupMealBookingMapper extends BaseMapper<GroupMealBooking> {
}
