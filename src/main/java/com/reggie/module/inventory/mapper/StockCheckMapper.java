package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockCheck;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {
}
