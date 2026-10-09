package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.DeliveryFeeStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配送费阶梯规则 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface DeliveryFeeStepMapper extends BaseMapper<DeliveryFeeStep> {
}
