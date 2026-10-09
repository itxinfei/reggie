package com.reggie.module.ai.rag.service;

/**
 * 知识库异步索引服务（P5 RAG）
 * <p>文档保存/重建后提交异步任务：物理清旧切块 → 400 字/50 重叠切块 → embedding（失败降级无向量）
 * → 落库，状态 DRAFT → INDEXING → READY/FAILED。同一文档并发提交只执行一次。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface KnowledgeIndexService {

    /**
     * 提交异步索引任务（文档已处于索引中则忽略本次提交）。
     * @param docId 文档ID
     */
    void submitIndex(Long docId);
}
