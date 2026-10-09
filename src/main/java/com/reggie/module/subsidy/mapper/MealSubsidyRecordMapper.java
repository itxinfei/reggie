package com.reggie.module.subsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.subsidy.model.MealSubsidyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 餐补流水 Mapper
 * 幂等由 uk_subsidy_record_trade(trade_no, record_type) 唯一键保证，
 * 插入冲突（DuplicateKeyException）即视为重复请求，调用方按幂等成功处理。
 */
@Mapper
public interface MealSubsidyRecordMapper extends BaseMapper<MealSubsidyRecord> {
}
