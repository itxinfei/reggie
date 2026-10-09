package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AiAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI聊天图片附件 Mapper 接口
 *
 * @author reggie
 * @since 2026-09-20
 */
@Mapper
public interface AiAttachmentMapper extends BaseMapper<AiAttachment> {
}
