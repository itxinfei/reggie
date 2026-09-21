package com.reggie.module.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.CustomException;
import com.reggie.module.ai.rag.dto.KnowledgeDocDTO;
import com.reggie.module.ai.rag.mapper.AiKnowledgeChunkMapper;
import com.reggie.module.ai.rag.mapper.AiKnowledgeDocMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;
import com.reggie.module.ai.rag.service.AiKnowledgeService;
import com.reggie.module.ai.rag.service.KnowledgeIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 知识库管理服务实现（P5 RAG）。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    /** 单篇内容长度上限 */
    private static final int MAX_CONTENT_LENGTH = 20000;

    @Resource
    private AiKnowledgeDocMapper docMapper;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private KnowledgeIndexService indexService;

    @Override
    public IPage<AiKnowledgeDoc> adminPage(int page, int pageSize, String keyword,
                                           String audience, String status) {
        Page<AiKnowledgeDoc> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<AiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(AiKnowledgeDoc::getTitle, keyword.trim());
        }
        if (audience != null && !audience.trim().isEmpty()) {
            wrapper.eq(AiKnowledgeDoc::getAudience, audience.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(AiKnowledgeDoc::getStatus, status.trim());
        }
        wrapper.orderByDesc(AiKnowledgeDoc::getUpdateTime);
        return docMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", docMapper.selectCount(null));
        data.put("ready", docMapper.selectCount(
                new QueryWrapper<AiKnowledgeDoc>().eq("status", "READY")));
        data.put("indexing", docMapper.selectCount(
                new QueryWrapper<AiKnowledgeDoc>().in("status", Arrays.asList("DRAFT", "INDEXING"))));
        data.put("failed", docMapper.selectCount(
                new QueryWrapper<AiKnowledgeDoc>().eq("status", "FAILED")));
        return data;
    }

    @Override
    public Long createDoc(KnowledgeDocDTO dto) {
        String title = requireText(dto.getTitle(), "标题");
        String content = requireText(dto.getContent(), "内容");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException("内容长度不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }

        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setTitle(title);
        doc.setDocType(normalizeDocType(dto.getDocType()));
        doc.setAudience(normalizeAudience(dto.getAudience()));
        doc.setContent(content);
        doc.setStatus("DRAFT");
        doc.setChunkCount(0);
        docMapper.insert(doc);

        indexService.submitIndex(doc.getId());
        return doc.getId();
    }

    @Override
    public void updateDoc(KnowledgeDocDTO dto) {
        if (dto.getId() == null) {
            throw new CustomException("缺少文档ID");
        }
        AiKnowledgeDoc doc = docMapper.selectById(dto.getId());
        if (doc == null) {
            throw new CustomException("知识文档不存在");
        }

        String title = requireText(dto.getTitle(), "标题");
        String content = requireText(dto.getContent(), "内容");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException("内容长度不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }

        doc.setTitle(title);
        doc.setDocType(normalizeDocType(dto.getDocType()));
        doc.setAudience(normalizeAudience(dto.getAudience()));

        boolean contentChanged = !content.equals(doc.getContent());
        doc.setContent(content);
        if (contentChanged) {
            // 原文变更：旧切块失效，重新异步索引
            doc.setStatus("DRAFT");
            doc.setChunkCount(0);
            doc.setErrorMsg(null);
        }
        docMapper.updateById(doc);

        if (contentChanged) {
            indexService.submitIndex(doc.getId());
        }
    }

    @Override
    public void deleteDoc(Long id) {
        AiKnowledgeDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new CustomException("知识文档不存在");
        }
        chunkMapper.delete(new QueryWrapper<AiKnowledgeChunk>().eq("doc_id", id));
        docMapper.deleteById(id);
    }

    @Override
    public void reindex(Long id) {
        AiKnowledgeDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new CustomException("知识文档不存在");
        }
        indexService.submitIndex(id);
    }

    /**
     * 类型归一：仅承认 FAQ，其余一律 TEXT。
     */
    private String normalizeDocType(String docType) {
        return "FAQ".equals(docType) ? "FAQ" : "TEXT";
    }

    /**
     * 受众归一：仅承认 CUSTOMER/MERCHANT，其余一律 BOTH。
     */
    private String normalizeAudience(String audience) {
        if ("CUSTOMER".equals(audience) || "MERCHANT".equals(audience)) {
            return audience;
        }
        return "BOTH";
    }

    private String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new CustomException(fieldName + "不能为空");
        }
        return text.trim();
    }
}
