package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AIConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * AI对话会话 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {

    /**
     * 查询用户的对话列表（分页）
     *
     * @param userId 用户ID
     * @param isDeleted 是否删除
     * @param offset 偏移量
     * @param limit 条数
     * @return 会话列表
     */
    List<AIConversation> selectUserConversations(@Param("userId") Long userId,
                                                  @Param("isDeleted") Integer isDeleted,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);
}
