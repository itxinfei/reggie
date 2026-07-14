package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.MarketingMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 营销消息推送记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MarketingMessageMapper extends BaseMapper<MarketingMessage> {
}
