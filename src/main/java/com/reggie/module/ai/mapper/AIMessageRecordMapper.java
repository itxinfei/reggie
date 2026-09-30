package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AIMessageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * AI消息记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface AIMessageRecordMapper extends BaseMapper<AIMessageRecord> {

    /**
     * 查询会话消息列表
     *
     * @param conversationId 会话ID
     * @param tenantId 租户ID
     * @param isDeleted 是否删除
     * @return 消息列表
     */
    List<AIMessageRecord> selectByConversationId(@Param("conversationId") String conversationId,
                                                   @Param("tenantId") Long tenantId,
                                                   @Param("isDeleted") Integer isDeleted);

    /**
     * 用户本人最近的 user 消息中按前缀匹配的内容（输入联想-个人历史源）。
     *
     * @param prefix 已转义的前缀
     * @param limit  条数上限
     */
    List<String> selectPersonalSuggestions(@Param("userId") Long userId,
                                           @Param("tenantId") Long tenantId,
                                           @Param("prefix") String prefix,
                                           @Param("limit") int limit);

    /**
     * 租户内近 {@code since} 之后的热点 user 消息（按出现次数排序，输入联想-店铺热点源）。
     *
     * @param prefix 已转义的前缀
     * @param since  起始时间（含）
     */
    List<String> selectHotSuggestions(@Param("tenantId") Long tenantId,
                                      @Param("prefix") String prefix,
                                      @Param("since") java.time.LocalDateTime since,
                                      @Param("limit") int limit);
}
