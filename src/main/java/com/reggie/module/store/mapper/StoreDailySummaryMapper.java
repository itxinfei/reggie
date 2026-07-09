package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.store.model.StoreDailySummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店每日经营汇总Mapper
 */
@Mapper
public interface StoreDailySummaryMapper extends BaseMapper<StoreDailySummary> {
}
