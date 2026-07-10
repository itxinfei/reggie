package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
