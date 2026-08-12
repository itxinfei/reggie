package com.reggie.module.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.cost.model.CostRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成本记录 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-10
 */
@Mapper
public interface CostRecordMapper extends BaseMapper<CostRecord> {
}
