package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.MarketingMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import java.util.List;

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

    /**
     * 批量插入营销消息推送记录
     * 用于批量推送场景，避免逐条 INSERT 造成 N+1 问题
     *
     * @param messages 消息列表
     */
    @Insert("<script>" +
            "INSERT INTO marketing_message (tenant_id, campaign_id, user_id, push_type, title, content, status, create_time, is_deleted) " +
            "VALUES " +
            "<foreach collection='list' item='m' separator=','>" +
            "(#{m.tenantId}, #{m.campaignId}, #{m.userId}, #{m.pushType}, #{m.title}, #{m.content}, #{m.status}, NOW(), 0)" +
            "</foreach>" +
            "</script>")
    void insertBatchList(@Param("list") List<MarketingMessage> messages);
}
