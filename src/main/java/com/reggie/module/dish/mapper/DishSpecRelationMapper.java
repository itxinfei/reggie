package com.reggie.module.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dish.model.DishSpecRelation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜品规格关联 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface DishSpecRelationMapper extends BaseMapper<DishSpecRelation> {

    /**
     * 批量插入规格关联
     *
     * @param relations 关联列表
     */
    void insertBatch(List<DishSpecRelation> relations);
}

