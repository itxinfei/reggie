package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AIMessageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI消息记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface AIMessageRecordMapper extends BaseMapper<AIMessageRecord> {

    /**
     * 查询会话消息列表
     */
    List<AIMessageRecord> selectByConversationId(@Param("conversationId") String conversationId,
                                                   @Param("isDeleted") Integer isDeleted);
}
