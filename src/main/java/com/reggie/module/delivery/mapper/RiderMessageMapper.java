package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 骑手消息 Mapper。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Mapper
public interface RiderMessageMapper extends BaseMapper<RiderMessage> {
}
