package com.reggie.module.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.rag.mapper.AiKnowledgeChunkMapper;
import com.reggie.module.ai.rag.mapper.AiKnowledgeDocMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;
import com.reggie.module.ai.rag.service.EmbeddingService;
import com.reggie.module.ai.rag.service.KnowledgeIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 知识库异步索引实现（P5 RAG）。
 * <p>复用 aiExecutor（其 TaskDecorator 已传播租户 ThreadLocal）；
 * embedding 不可用或失败时切块照常落库（embedding=NULL），检索自动降级，不标记 FAILED。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class KnowledgeIndexServiceImpl implements KnowledgeIndexService {

    /** 切块大小（字符） */
    static final int CHUNK_SIZE = 400;

    /** 相邻切块重叠（字符） */
    static final int CHUNK_OVERLAP = 50;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 索引中的文档，防止并发重建竞态 */
    private final Set<Long> indexingDocs = ConcurrentHashMap.newKeySet();

    @Resource(name = "aiExecutor")
    private Executor aiExecutor;

    @Resource
    private AiKnowledgeDocMapper docMapper;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Override
    public void submitIndex(final Long docId) {
        if (docId == null || !indexingDocs.add(docId)) {
            // 已在索引中：忽略重复提交
            return;
        }
        aiExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doIndex(docId);
                } finally {
                    indexingDocs.remove(docId);
                }
            }
        });
    }

    /**
     * 索引主流程：标记 INDEXING → 物理清旧切块 → 切块 → embedding（可降级）→ 落库 → READY。
     */
    private void doIndex(Long docId) {
        AiKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            // 文档在索引前被删除
            return;
        }
        try {
            doc.setStatus("INDEXING");
            doc.setErrorMsg(null);
            docMapper.updateById(doc);

            // 重建：物理删除旧切块（chunk 表无逻辑删除）
            chunkMapper.delete(new QueryWrapper<AiKnowledgeChunk>().eq("doc_id", docId));

            List<String> pieces = splitToChunks(doc.getTitle(), doc.getContent());
            if (pieces.isEmpty()) {
                throw new IllegalStateException("文档内容为空，无法索引");
            }

            String warnMsg = indexChunks(docId, pieces);

            AiKnowledgeDoc ready = new AiKnowledgeDoc();
            ready.setId(docId);
            ready.setStatus("READY");
            ready.setChunkCount(pieces.size());
            // embedding 未配置/部分失败时保留降级提示，便于页面提示当前检索方式
            ready.setErrorMsg(truncate(warnMsg, 480));
            docMapper.updateById(ready);
            log.info("知识库索引完成: docId={}, chunks={}", docId, pieces.size());
        } catch (Exception e) {
            log.error("知识库索引失败: docId={}", docId, e);
            AiKnowledgeDoc failed = new AiKnowledgeDoc();
            failed.setId(docId);
            failed.setStatus("FAILED");
            failed.setErrorMsg(truncate(e.getMessage(), 480));
            docMapper.updateById(failed);
        }
    }

    /**
     * 切块落库（含向量），返回降级警告信息（无警告为 null）。
     */
    private String indexChunks(Long docId, List<String> pieces) {
        boolean embeddingAvailable = embeddingService.isEmbeddingAvailable();
        String embedModel = embeddingAvailable ? embeddingService.getEmbeddingModel() : null;
        if (!embeddingAvailable) {
            for (int i = 0; i < pieces.size(); i++) {
                insertChunk(docId, i, pieces.get(i), null, null);
            }
            return "未配置embedding模型，当前仅支持全文检索";
        }

        String warnMsg = null;
        for (int offset = 0; offset < pieces.size(); offset += EmbeddingServiceImpl.BATCH_SIZE) {
            int end = Math.min(offset + EmbeddingServiceImpl.BATCH_SIZE, pieces.size());
            List<String> batch = pieces.subList(offset, end);
            List<double[]> vectors = null;
            try {
                vectors = embeddingService.embed(batch);
            } catch (Exception e) {
                warnMsg = "向量生成失败，已降级全文检索: " + e.getMessage();
                log.warn("知识库批量向量生成失败，本批降级无向量: docId={}, size={}, reason={}",
                        docId, batch.size(), e.getMessage());
            }
            for (int j = 0; j < batch.size(); j++) {
                String embeddingJson = null;
                if (vectors != null && vectors.get(j) != null) {
                    try {
                        embeddingJson = MAPPER.writeValueAsString(vectors.get(j));
                    } catch (Exception e) {
                        log.warn("向量序列化失败: docId={}, chunk={}", docId, offset + j);
                    }
                }
                insertChunk(docId, offset + j, batch.get(j), embedModel, embeddingJson);
            }
        }
        return warnMsg;
    }

    private void insertChunk(Long docId, int index, String content, String embedModel, String embeddingJson) {
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setDocId(docId);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setEmbedModel(embedModel);
        chunk.setEmbedding(embeddingJson);
        chunkMapper.insert(chunk);
    }

    /**
     * 文本切块（语义边界优先）：
     * <ul>
     *   <li>每个切块带文档标题前缀——检索命中后模型能看到出处，不再是无头文本；</li>
     *   <li>段落（按换行切）逐段累积进同一块直到预算用满，FAQ 的问答对不再被拦腰切断；</li>
     *   <li>单段超预算时对该段做 400 字滑窗（重叠 {@value #CHUNK_OVERLAP}）兜底。</li>
     * </ul>
     * 包级可见以便单元测试。
     */
    static List<String> splitToChunks(String title, String content) {
        List<String> chunks = new java.util.ArrayList<>();
        if (content == null) {
            return chunks;
        }
        String text = content.trim();
        if (text.isEmpty()) {
            return chunks;
        }
        // 标题前缀：挤占切块预算，超长标题（>80字）不拼，避免正文预算被吃光
        String prefix = "";
        if (title != null && !title.trim().isEmpty() && title.trim().length() <= 80) {
            prefix = title.trim() + "\n";
        }
        int budget = CHUNK_SIZE - prefix.length();

        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n+")) {
            String p = paragraph.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() > budget) {
                // 单段超预算：先落已累积内容，再对该段滑窗兜底
                flushChunk(chunks, current, prefix);
                slidingWindow(p, budget, chunks, prefix);
                continue;
            }
            if (current.length() > 0 && current.length() + 1 + p.length() > budget) {
                flushChunk(chunks, current, prefix);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(p);
        }
        flushChunk(chunks, current, prefix);
        return chunks;
    }

    /** 落一个切块（带标题前缀） */
    private static void flushChunk(List<String> chunks, StringBuilder current, String prefix) {
        if (current.length() > 0) {
            chunks.add(prefix + current);
            current.setLength(0);
        }
    }

    /** 单段超预算的滑窗兜底：400 字窗口、相邻重叠 50 字 */
    private static void slidingWindow(String paragraph, int window, List<String> chunks, String prefix) {
        int step = Math.max(1, window - CHUNK_OVERLAP);
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + window, paragraph.length());
            String piece = paragraph.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(prefix + piece);
            }
            if (end == paragraph.length()) {
                break;
            }
            start += step;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
