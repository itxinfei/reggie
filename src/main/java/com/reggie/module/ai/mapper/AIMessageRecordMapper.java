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
     * @param isDeleted 是否删除
     * @return 消息列表
     */
    List<AIMessageRecord> selectByConversationId(@Param("conversationId") String conversationId,
                                                   @Param("isDeleted") Integer isDeleted);
}
