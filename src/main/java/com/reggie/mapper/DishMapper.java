package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
