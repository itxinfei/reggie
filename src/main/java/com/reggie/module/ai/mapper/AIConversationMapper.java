package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AIConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话会话 Mapper
 *
 * @author reggie
 * @since 2026-07-10
 */
@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {

    /**
     * 查询用户的对话列表（分页）
     */
    List<AIConversation> selectUserConversations(@Param("userId") Long userId,
                                                  @Param("isDeleted") Integer isDeleted,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);
}
