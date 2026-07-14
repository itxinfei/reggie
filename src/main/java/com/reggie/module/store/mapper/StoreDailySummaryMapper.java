package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.store.model.StoreDailySummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 门店每日经营汇总 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StoreDailySummaryMapper extends BaseMapper<StoreDailySummary> {
}
