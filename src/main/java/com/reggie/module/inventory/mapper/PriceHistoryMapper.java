package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.PriceHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 价格历史 Mapper 接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Mapper
public interface PriceHistoryMapper extends BaseMapper<PriceHistory> {
}
