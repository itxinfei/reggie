package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.DishSpecRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品规格关联 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface DishSpecRelationMapper extends BaseMapper<DishSpecRelation> {
}
