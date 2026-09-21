package com.reggie.module.ai.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.module.ai.rag.dto.KnowledgeDocDTO;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;

import java.util.Map;

/**
 * AI 知识库管理服务（P5 RAG）
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface AiKnowledgeService {

    /**
     * 后台分页查询。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 标题关键词
     * @param audience 受众
     * @param status 状态
     * @return 分页
     */
    IPage<AiKnowledgeDoc> adminPage(int page, int pageSize, String keyword, String audience, String status);

    /**
     * 状态统计（total/ready/indexing/failed）。
     * @return 统计
     */
    Map<String, Object> stats();

    /**
     * 新增文档并触发异步索引。
     * @param dto 内容
     * @return 新文档ID
     */
    Long createDoc(KnowledgeDocDTO dto);

    /**
     * 更新文档；原文变更时重新索引。
     * @param dto 内容
     */
    void updateDoc(KnowledgeDocDTO dto);

    /**
     * 删除文档及其全部切块。
     * @param id 文档ID
     */
    void deleteDoc(Long id);

    /**
     * 手动重建索引。
     * @param id 文档ID
     */
    void reindex(Long id);
}
