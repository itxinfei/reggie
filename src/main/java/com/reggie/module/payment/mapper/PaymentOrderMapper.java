package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 支付单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
