package com.reggie.module.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 知识库切块 Mapper（P5）
 * <p>FULLTEXT 检索依赖 MySQL 8.x 内置 ngram parser；tenant_id 条件由租户插件自动追加。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunk> {

    /**
     * ngram FULLTEXT 候选预取（自然语言模式，按相关性降序）。
     *
     * @param query    原始查询（ngram parser 自动按 bigram 分词）
     * @param audience 受众：CUSTOMER/MERCHANT（BOTH 文档始终可见）
     * @param limit    候选上限
     * @return 候选切块
     */
    @Select("SELECT c.id, c.tenant_id, c.doc_id, c.chunk_index, c.content, c.embedding, c.embed_model, "
            + "c.create_time, c.update_time "
            + "FROM ai_knowledge_chunk c "
            + "INNER JOIN ai_knowledge_doc d ON c.doc_id = d.id "
            + "WHERE d.status = 'READY' "
            + "AND (d.audience = #{audience} OR d.audience = 'BOTH') "
            + "AND MATCH (c.content) AGAINST (#{query} IN NATURAL LANGUAGE MODE) "
            + "ORDER BY MATCH (c.content) AGAINST (#{query} IN NATURAL LANGUAGE MODE) DESC, c.id "
            + "LIMIT #{limit}")
    List<AiKnowledgeChunk> fulltextSearch(@Param("query") String query,
                                          @Param("audience") String audience,
                                          @Param("limit") int limit);

    /**
     * FULLTEXT 不可用/无命中时的 LIKE 兜底。
     *
     * @param keyword 关键词
     * @param audience 受众
     * @param limit 上限
     * @return 命中切块
     */
    @Select("SELECT c.id, c.tenant_id, c.doc_id, c.chunk_index, c.content, c.embedding, c.embed_model, "
            + "c.create_time, c.update_time "
            + "FROM ai_knowledge_chunk c "
            + "INNER JOIN ai_knowledge_doc d ON c.doc_id = d.id "
            + "WHERE d.status = 'READY' "
            + "AND (d.audience = #{audience} OR d.audience = 'BOTH') "
            + "AND c.content LIKE CONCAT('%', #{keyword}, '%') "
            + "ORDER BY c.chunk_index, c.id "
            + "LIMIT #{limit}")
    List<AiKnowledgeChunk> likeSearch(@Param("keyword") String keyword,
                                      @Param("audience") String audience,
                                      @Param("limit") int limit);
}
