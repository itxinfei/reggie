package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderIncomeLedger;
import org.apache.ibatis.annotations.Mapper;

/**
 * 骑手收入流水 Mapper（幂等入账）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Mapper
public interface RiderIncomeLedgerMapper extends BaseMapper<RiderIncomeLedger> {
}
