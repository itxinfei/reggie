package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存变动记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface StockRecordMapper extends BaseMapper<StockRecord> {
}
