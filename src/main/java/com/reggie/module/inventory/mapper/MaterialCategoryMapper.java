package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.MaterialCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原料分类 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface MaterialCategoryMapper extends BaseMapper<MaterialCategory> {
}
