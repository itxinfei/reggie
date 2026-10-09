package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.AiPromptTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 提示词模板 Mapper
 *
 * @author reggie
 * @since 2026-09-21
 */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
