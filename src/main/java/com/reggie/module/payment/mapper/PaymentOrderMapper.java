package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
