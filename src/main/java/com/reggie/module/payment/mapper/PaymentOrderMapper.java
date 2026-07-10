package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
