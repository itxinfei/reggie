package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockCheck;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 盘点记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {
}
