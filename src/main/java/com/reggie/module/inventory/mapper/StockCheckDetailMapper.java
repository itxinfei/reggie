package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockCheckDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点明细 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface StockCheckDetailMapper extends BaseMapper<StockCheckDetail> {
}
