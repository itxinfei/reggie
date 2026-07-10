package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.MarketingMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销消息推送记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface MarketingMessageMapper extends BaseMapper<MarketingMessage> {
}
