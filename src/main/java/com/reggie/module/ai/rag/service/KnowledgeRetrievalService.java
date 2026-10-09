package com.reggie.module.ai.rag.service;

import java.util.List;

/**
 * 知识库检索服务（P5 RAG）
 * <p>三级降级：ngram FULLTEXT 预取候选 + 内存向量余弦 TopN → 无向量时 FULLTEXT 相关性排序 →
 * LIKE 兜底；任何异常返回空列表，绝不阻断聊天。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface KnowledgeRetrievalService {

    /**
     * 按查询检索知识片段。
     *
     * @param query    用户问题
     * @param audience 受众：CUSTOMER/MERCHANT（BOTH 文档始终可见）
     * @param limit    返回片段上限
     * @return 片段文本列表（可能为空）
     */
    List<String> retrieve(String query, String audience, int limit);

    /**
     * 检索并拼装可直接注入 system 消息的文本；无命中返回 null。
     *
     * @param query    用户问题
     * @param audience 受众
     * @return 注入文本或 null
     */
    String buildKnowledgePrompt(String query, String audience);
}
