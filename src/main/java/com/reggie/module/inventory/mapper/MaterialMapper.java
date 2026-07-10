package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原料 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
