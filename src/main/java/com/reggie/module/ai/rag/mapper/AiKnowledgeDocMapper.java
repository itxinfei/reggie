package com.reggie.module.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库文档 Mapper（P5）
 *
 * @author reggie
 * @since 2026-09-21
 */
@Mapper
public interface AiKnowledgeDocMapper extends BaseMapper<AiKnowledgeDoc> {
}
