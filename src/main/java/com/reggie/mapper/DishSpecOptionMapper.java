package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.DishSpecOption;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品规格选项 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface DishSpecOptionMapper extends BaseMapper<DishSpecOption> {
}
