package com.reggie.module.ai.rag.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.rag.mapper.AiKnowledgeChunkMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import com.reggie.module.ai.rag.service.EmbeddingService;
import com.reggie.module.ai.rag.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 知识库检索实现（P5 RAG）：向量余弦 → FULLTEXT → LIKE 三级降级。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    /** FULLTEXT 预取候选上限 */
    private static final int CANDIDATE_LIMIT = 200;

    /** LIKE 兜底关键词长度上限 */
    private static final int LIKE_KEYWORD_LIMIT = 50;

    /** 返回片段数 */
    private static final int DEFAULT_LIMIT = 4;

    /**
     * 向量命中的最低余弦相似度：FULLTEXT 预取的候选里必然混入弱相关内容，
     * 低于该阈值的不注入 prompt（宁可不给 RAG 上下文，也不给噪声——
     * buildKnowledgePrompt 拿到空结果会自动跳过注入）。阈值取保守值 0.2，
     * 兼容常见 embedding 模型的相似度分布；若全被过滤，说明知识库确实无相关内容。
     */
    static final double MIN_COSINE_SIMILARITY = 0.2;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Override
    public List<String> retrieve(String query, String audience, int limit) {
        try {
            if (query == null || audience == null) {
                return new ArrayList<>();
            }
            String normalizedQuery = query.trim();
            String normalizedAudience = audience.trim();
            if (normalizedQuery.isEmpty() || normalizedAudience.isEmpty() || limit <= 0) {
                return new ArrayList<>();
            }

            // 1) ngram FULLTEXT 候选预取（相关性降序）
            List<AiKnowledgeChunk> candidates = chunkMapper
                    .fulltextSearch(normalizedQuery, normalizedAudience, CANDIDATE_LIMIT);

            // 2) 向量余弦重排（仅与当前模型一致、维度匹配的候选参与）
            List<ScoredSnippet> vectorScored = vectorRerank(normalizedQuery, candidates);
            if (vectorScored != null) {
                return toContents(vectorScored, limit);
            }

            // 3) 无向量能力/查询向量化失败/候选向量全失效：按 FULLTEXT 相关性顺序
            if (!candidates.isEmpty()) {
                List<String> result = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, candidates.size()); i++) {
                    result.add(candidates.get(i).getContent());
                }
                return result;
            }

            // 4) FULLTEXT 无命中（短查询/停用词等）：LIKE 兜底
            String keyword = normalizedQuery.length() > LIKE_KEYWORD_LIMIT
                    ? normalizedQuery.substring(0, LIKE_KEYWORD_LIMIT) : normalizedQuery;
            List<AiKnowledgeChunk> liked = chunkMapper
                    .likeSearch(keyword, normalizedAudience, limit);
            List<String> result = new ArrayList<>();
            for (AiKnowledgeChunk chunk : liked) {
                result.add(chunk.getContent());
            }
            return result;
        } catch (Exception e) {
            // 宽异常兜底：检索失败绝不阻断聊天
            log.warn("知识库检索异常，返回空结果: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String buildKnowledgePrompt(String query, String audience) {
        List<String> snippets = retrieve(query, audience, DEFAULT_LIMIT);
        if (snippets.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("以下内容来自商家知识库，请优先依据这些内容回答用户问题；若与问题无关则忽略：");
        for (int i = 0; i < snippets.size(); i++) {
            sb.append("\n【知识片段").append(i + 1).append("】").append(snippets.get(i));
        }
        return sb.toString();
    }

    /**
     * 查询向量化 + 候选余弦打分。
     *
     * @return 打分列表；null 表示向量链路不可用（调用方应降级 FULLTEXT 顺序）
     */
    private List<ScoredSnippet> vectorRerank(String query, List<AiKnowledgeChunk> candidates) {
        if (!embeddingService.isEmbeddingAvailable()) {
            return null;
        }
        double[] queryVector;
        String currentModel;
        try {
            List<double[]> queryVectors = embeddingService
                    .embed(Collections.singletonList(query));
            queryVector = queryVectors.get(0);
            currentModel = embeddingService.getEmbeddingModel();
        } catch (Exception e) {
            log.warn("查询向量化失败，降级全文检索: {}", e.getMessage());
            return null;
        }

        List<ScoredSnippet> scored = new ArrayList<>();
        for (AiKnowledgeChunk chunk : candidates) {
            if (chunk.getEmbedding() == null || chunk.getEmbedModel() == null
                    || !chunk.getEmbedModel().equals(currentModel)) {
                // 旧模型向量或无向量切块：不参与余弦
                continue;
            }
            try {
                double[] vector = MAPPER.readValue(chunk.getEmbedding(), double[].class);
                if (vector.length != queryVector.length) {
                    continue;
                }
                scored.add(new ScoredSnippet(cosine(queryVector, vector), chunk.getContent()));
            } catch (Exception e) {
                log.warn("切块向量解析失败，跳过: chunkId={}", chunk.getId());
            }
        }
        return scored;
    }

    private List<String> toContents(List<ScoredSnippet> scored, int limit) {
        Collections.sort(scored, new Comparator<ScoredSnippet>() {
            @Override
            public int compare(ScoredSnippet a, ScoredSnippet b) {
                return Double.compare(b.score, a.score);
            }
        });
        List<String> result = new ArrayList<>();
        for (ScoredSnippet s : scored) {
            if (result.size() >= limit) {
                break;
            }
            // 已按相似度降序：首个低于阈值即可停，弱相关候选不注入 prompt
            if (s.score < MIN_COSINE_SIMILARITY) {
                log.debug("余弦低于阈值({}), 剩余{}个候选全部过滤", MIN_COSINE_SIMILARITY,
                        scored.size() - result.size());
                break;
            }
            result.add(s.content);
        }
        return result;
    }

    /**
     * 余弦相似度。
     */
    private double cosine(double[] a, double[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / Math.sqrt(normA * normB);
    }

    /** 打分片段（内部传输） */
    private static class ScoredSnippet {
        private final double score;
        private final String content;

        ScoredSnippet(double score, String content) {
            this.score = score;
            this.content = content;
        }
    }
}
