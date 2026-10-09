package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderExceptionOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 骑手异常工单 Mapper。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Mapper
public interface RiderExceptionOrderMapper extends BaseMapper<RiderExceptionOrder> {
}
