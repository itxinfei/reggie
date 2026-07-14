package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 退款记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
