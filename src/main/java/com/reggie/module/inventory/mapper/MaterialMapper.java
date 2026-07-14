package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 原料 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
