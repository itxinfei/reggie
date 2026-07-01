package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockCheck;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {
}
