package com.reggie.module.ai.rag.service.impl;

import com.reggie.module.ai.rag.mapper.AiKnowledgeChunkMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import com.reggie.module.ai.rag.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeRetrievalServiceImpl} 单元测试：向量余弦重排、模型/维度失效过滤、
 * 无 embedding 走 FULLTEXT 顺序、候选空走 LIKE（长查询截断 50）、异常返空。
 *
 * @author reggie
 * @since 2026-09-21
 */
class KnowledgeRetrievalServiceImplTest {

    private KnowledgeRetrievalServiceImpl service;
    private AiKnowledgeChunkMapper chunkMapper;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        service = new KnowledgeRetrievalServiceImpl();
        chunkMapper = mock(AiKnowledgeChunkMapper.class);
        embeddingService = mock(EmbeddingService.class);
        ReflectionTestUtils.setField(service, "chunkMapper", chunkMapper);
        ReflectionTestUtils.setField(service, "embeddingService", embeddingService);

        // 默认 FULLTEXT 无命中、LIKE 无命中
        when(chunkMapper.fulltextSearch(eq("查询"), anyStringSafe(), anyInt()))
                .thenReturn(new ArrayList<AiKnowledgeChunk>());
        when(chunkMapper.likeSearch(eq("查询"), anyStringSafe(), anyInt()))
                .thenReturn(new ArrayList<AiKnowledgeChunk>());
    }

    /** 仅为让 anyString 静态导入集中的别名 */
    private static String anyStringSafe() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    @Test
    void vectorRerankOrdersByCosineAndAppliesLimit() {
        enableVector(new double[] {1.0d, 0.0d});
        // A 与查询同向 cos=1；C 斜向 cos≈0.707；B 正交 cos=0
        List<AiKnowledgeChunk> candidates = Arrays.asList(
                chunk(1L, "内容B", "[0.0,1.0]", "emb-v1"),
                chunk(2L, "内容C", "[0.70710678,0.70710678]", "emb-v1"),
                chunk(3L, "内容A", "[1.0,0.0]", "emb-v1"));
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenReturn(candidates);

        List<String> result = service.retrieve("查询", "BOTH", 2);
        assertEquals(Arrays.asList("内容A", "内容C"), result);
    }

    @Test
    void vectorRerankSkipsStaleModelAndNullVectors() {
        enableVector(new double[] {1.0d, 0.0d});
        List<AiKnowledgeChunk> candidates = Arrays.asList(
                chunk(1L, "无向量", null, null),
                chunk(2L, "旧模型", "[1.0,0.0]", "old-model"),
                chunk(3L, "内容A", "[1.0,0.0]", "emb-v1"),
                chunk(4L, "维度不符", "[1.0,0.0,0.0]", "emb-v1"));
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenReturn(candidates);

        List<String> result = service.retrieve("查询", "BOTH", 4);
        assertEquals(Collections.singletonList("内容A"), result);
    }

    @Test
    void allStaleVectorsReturnEmptyFromVectorPath() {
        // 现行行为：候选全部不参与余弦时 scored 为空列表（非 null），直接返回空，不再回落
        enableVector(new double[] {1.0d, 0.0d});
        List<AiKnowledgeChunk> candidates = Collections.singletonList(
                chunk(1L, "旧模型", "[1.0,0.0]", "old-model"));
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenReturn(candidates);
        assertTrue(service.retrieve("查询", "BOTH", 4).isEmpty());
    }

    @Test
    void noEmbeddingFallsBackToFulltextOrder() {
        when(embeddingService.isEmbeddingAvailable()).thenReturn(false);
        List<AiKnowledgeChunk> candidates = Arrays.asList(
                chunk(1L, "全文第一", null, null),
                chunk(2L, "全文第二", null, null));
        when(chunkMapper.fulltextSearch(eq("查询"), eq("MERCHANT"), eq(200)))
                .thenReturn(candidates);

        List<String> result = service.retrieve("查询", "MERCHANT", 1);
        assertEquals(Collections.singletonList("全文第一"), result);
    }

    @Test
    void queryVectorizationFailureFallsBackToFulltext() {
        when(embeddingService.isEmbeddingAvailable()).thenReturn(true);
        when(embeddingService.embed(anyList())).thenThrow(new IllegalStateException("500"));
        List<AiKnowledgeChunk> candidates = Collections.singletonList(
                chunk(1L, "全文片段", null, null));
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenReturn(candidates);

        assertEquals(Collections.singletonList("全文片段"),
                service.retrieve("查询", "BOTH", 4));
    }

    @Test
    void emptyCandidatesFallBackToLike() {
        List<String> result = service.retrieve("查询", "BOTH", 4);
        assertTrue(result.isEmpty());
        verify(chunkMapper).likeSearch(eq("查询"), eq("BOTH"), eq(4));
    }

    @Test
    void longQueryKeywordTruncatedForLike() {
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            q.append('字');
        }
        String expectedKeyword = q.substring(0, 50);
        when(chunkMapper.fulltextSearch(eq(q.toString()), eq("BOTH"), eq(200)))
                .thenReturn(new ArrayList<AiKnowledgeChunk>());
        when(chunkMapper.likeSearch(eq(expectedKeyword), eq("BOTH"), eq(4)))
                .thenReturn(Collections.singletonList(chunk(9L, "LIKE命中", null, null)));

        List<String> result = service.retrieve(q.toString(), "BOTH", 4);
        assertEquals(Collections.singletonList("LIKE命中"), result);
    }

    @Test
    void mapperExceptionReturnsEmpty() {
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenThrow(new RuntimeException("sql error"));
        assertTrue(service.retrieve("查询", "BOTH", 4).isEmpty());
    }

    @Test
    void invalidArgumentsReturnEmpty() {
        assertTrue(service.retrieve(null, "BOTH", 4).isEmpty());
        assertTrue(service.retrieve("查询", null, 4).isEmpty());
        assertTrue(service.retrieve("  ", "BOTH", 4).isEmpty());
        assertTrue(service.retrieve("查询", "  ", 4).isEmpty());
        assertTrue(service.retrieve("查询", "BOTH", 0).isEmpty());
    }

    @Test
    void buildKnowledgePromptWrapsSnippets() {
        when(embeddingService.isEmbeddingAvailable()).thenReturn(false);
        when(chunkMapper.fulltextSearch(eq("查询"), eq("BOTH"), eq(200)))
                .thenReturn(Collections.singletonList(chunk(1L, "知识点", null, null)));
        String prompt = service.buildKnowledgePrompt("查询", "BOTH");
        assertTrue(prompt.startsWith("以下内容来自商家知识库"));
        assertTrue(prompt.contains("【知识片段1】知识点"));
    }

    @Test
    void buildKnowledgePromptNullWhenNoSnippet() {
        assertNull(service.buildKnowledgePrompt("查询", "BOTH"));
    }

    // ==================== 辅助 ====================

    private void enableVector(double[] queryVector) {
        when(embeddingService.isEmbeddingAvailable()).thenReturn(true);
        when(embeddingService.getEmbeddingModel()).thenReturn("emb-v1");
        when(embeddingService.embed(anyList()))
                .thenReturn(Collections.singletonList(queryVector));
    }

    private AiKnowledgeChunk chunk(Long id, String content, String embedding, String embedModel) {
        AiKnowledgeChunk c = new AiKnowledgeChunk();
        c.setId(id);
        c.setContent(content);
        c.setEmbedding(embedding);
        c.setEmbedModel(embedModel);
        return c;
    }
}
