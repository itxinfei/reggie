package com.reggie.module.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.cashier.model.CashierRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收银记录 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-10
 */
@Mapper
public interface CashierRecordMapper extends BaseMapper<CashierRecord> {
}
