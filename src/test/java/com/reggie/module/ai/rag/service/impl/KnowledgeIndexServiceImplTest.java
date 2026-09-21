package com.reggie.module.ai.rag.service.impl;

import com.reggie.module.ai.rag.mapper.AiKnowledgeChunkMapper;
import com.reggie.module.ai.rag.mapper.AiKnowledgeDocMapper;
import com.reggie.module.ai.rag.model.AiKnowledgeChunk;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;
import com.reggie.module.ai.rag.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeIndexServiceImpl} 单元测试：反射验证 400/50 滑窗切块；
 * 同步执行器驱动索引主流程，覆盖向量落库、embedding 不可用/失败降级 READY、
 * 空内容与落库异常 FAILED、并发重建去重。
 *
 * @author reggie
 * @since 2026-09-21
 */
class KnowledgeIndexServiceImplTest {

    private KnowledgeIndexServiceImpl service;
    private AiKnowledgeDocMapper docMapper;
    private AiKnowledgeChunkMapper chunkMapper;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        service = new KnowledgeIndexServiceImpl();
        docMapper = mock(AiKnowledgeDocMapper.class);
        chunkMapper = mock(AiKnowledgeChunkMapper.class);
        embeddingService = mock(EmbeddingService.class);

        ReflectionTestUtils.setField(service, "docMapper", docMapper);
        ReflectionTestUtils.setField(service, "chunkMapper", chunkMapper);
        ReflectionTestUtils.setField(service, "embeddingService", embeddingService);
        // 默认同步执行，submitIndex 内 Runnable 在当前线程跑完
        ReflectionTestUtils.setField(service, "aiExecutor", new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        });

        when(docMapper.updateById(any(AiKnowledgeDoc.class))).thenReturn(1);
        when(chunkMapper.delete(any())).thenReturn(0);
        when(chunkMapper.insert(any(AiKnowledgeChunk.class))).thenReturn(1);
    }

    // ==================== splitToChunks（反射） ====================

    @SuppressWarnings("unchecked")
    private List<String> split(String content) throws Exception {
        Method method = KnowledgeIndexServiceImpl.class
                .getDeclaredMethod("splitToChunks", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, content);
    }

    @Test
    void splitNullReturnsEmpty() throws Exception {
        assertTrue(split(null).isEmpty());
    }

    @Test
    void splitBlankReturnsEmpty() throws Exception {
        assertTrue(split("   \n  ").isEmpty());
    }

    @Test
    void splitShortTextSingleTrimmedChunk() throws Exception {
        List<String> chunks = split("  短文本  ");
        assertEquals(1, chunks.size());
        assertEquals("短文本", chunks.get(0));
    }

    @Test
    void splitExactlyChunkSizeSingleChunk() throws Exception {
        assertEquals(1, split(patterned(400)).size());
    }

    @Test
    void splitOneOverSizeProducesTwoChunks() throws Exception {
        List<String> chunks = split(patterned(401));
        assertEquals(2, chunks.size());
        assertEquals(51, chunks.get(1).length());
    }

    @Test
    void splitLongTextSlidesWithOverlap() throws Exception {
        List<String> chunks = split(patterned(800));
        // step=350：[0,400] [350,750] [700,800]
        assertEquals(3, chunks.size());
        assertEquals(400, chunks.get(0).length());
        assertEquals(400, chunks.get(1).length());
        assertEquals(100, chunks.get(2).length());
        // 块1 前 50 字必须等于块0 尾 50 字（滑窗重叠，位置 350-400）
        assertEquals(chunks.get(0).substring(350), chunks.get(1).substring(0, 50));
    }

    // ==================== doIndex 主流程 ====================

    @Test
    void indexWithEmbeddingStoresVectorsAndReady() {
        AiKnowledgeDoc doc = doc(1L, patterned(800));
        when(docMapper.selectById(1L)).thenReturn(doc);
        when(embeddingService.isEmbeddingAvailable()).thenReturn(true);
        when(embeddingService.getEmbeddingModel()).thenReturn("emb-v1");
        List<double[]> vectors = vectors(3);
        when(embeddingService.embed(anyList())).thenReturn(vectors);

        service.submitIndex(1L);

        ArgumentCaptor<AiKnowledgeChunk> chunkCap = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper, times(3)).insert(chunkCap.capture());
        List<AiKnowledgeChunk> inserted = chunkCap.getAllValues();
        for (int i = 0; i < 3; i++) {
            assertEquals(i, inserted.get(i).getChunkIndex());
            assertEquals("emb-v1", inserted.get(i).getEmbedModel());
            assertTrue(inserted.get(i).getEmbedding().startsWith("["));
        }

        AiKnowledgeDoc saved = captureLastDoc();
        assertEquals("READY", saved.getStatus());
        assertEquals(Integer.valueOf(3), saved.getChunkCount());
        assertNull(saved.getErrorMsg());
    }

    @Test
    void indexWithoutEmbeddingDegradesToReadyNoVectors() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, "一份知识"));
        when(embeddingService.isEmbeddingAvailable()).thenReturn(false);

        service.submitIndex(1L);

        ArgumentCaptor<AiKnowledgeChunk> chunkCap = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper).insert(chunkCap.capture());
        assertNull(chunkCap.getValue().getEmbedding());
        assertNull(chunkCap.getValue().getEmbedModel());

        AiKnowledgeDoc saved = captureLastDoc();
        assertEquals("READY", saved.getStatus());
        assertTrue(saved.getErrorMsg().contains("仅支持全文检索"));
    }

    @Test
    void indexWhenEmbedThrowsDegradesReadyWithWarn() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, "知识"));
        when(embeddingService.isEmbeddingAvailable()).thenReturn(true);
        when(embeddingService.getEmbeddingModel()).thenReturn("emb-v1");
        when(embeddingService.embed(anyList())).thenThrow(new IllegalStateException("上游500"));

        service.submitIndex(1L);

        ArgumentCaptor<AiKnowledgeChunk> chunkCap = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper).insert(chunkCap.capture());
        assertNull(chunkCap.getValue().getEmbedding());

        AiKnowledgeDoc saved = captureLastDoc();
        assertEquals("READY", saved.getStatus());
        assertTrue(saved.getErrorMsg().contains("向量生成失败"));
    }

    @Test
    void indexEmptyContentMarksFailed() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, "   "));
        service.submitIndex(1L);

        AiKnowledgeDoc saved = captureLastDoc();
        assertEquals("FAILED", saved.getStatus());
        assertTrue(saved.getErrorMsg().contains("内容为空"));
        verify(chunkMapper, never()).insert(any(AiKnowledgeChunk.class));
    }

    @Test
    void indexMissingDocSilentlyReturns() {
        when(docMapper.selectById(1L)).thenReturn(null);
        service.submitIndex(1L);
        verify(docMapper, never()).updateById(any(AiKnowledgeDoc.class));
        verify(chunkMapper, never()).insert(any(AiKnowledgeChunk.class));
    }

    @Test
    void indexWhenChunkInsertThrowsMarksFailed() {
        when(docMapper.selectById(1L)).thenReturn(doc(1L, "知识"));
        when(embeddingService.isEmbeddingAvailable()).thenReturn(false);
        when(chunkMapper.insert(any(AiKnowledgeChunk.class)))
                .thenThrow(new RuntimeException("disk full"));

        service.submitIndex(1L);

        AiKnowledgeDoc saved = captureLastDoc();
        assertEquals("FAILED", saved.getStatus());
        assertTrue(saved.getErrorMsg().contains("disk full"));
    }

    @Test
    void duplicateSubmitWhileIndexingIsIgnored() {
        final List<Runnable> queued = new ArrayList<Runnable>();
        ReflectionTestUtils.setField(service, "aiExecutor", new Executor() {
            @Override
            public void execute(Runnable r) {
                queued.add(r); // 先缓存不运行，保持「索引中」状态
            }
        });
        when(docMapper.selectById(1L)).thenReturn(doc(1L, "知识"));
        when(embeddingService.isEmbeddingAvailable()).thenReturn(false);

        service.submitIndex(1L); // 首次入列
        service.submitIndex(1L); // 并发重复提交，必须被忽略
        assertEquals(1, queued.size());

        // 放行首次任务，正常完成
        queued.get(0).run();
        assertEquals("READY", captureLastDoc().getStatus());
    }

    // ==================== 辅助 ====================

    private AiKnowledgeDoc captureLastDoc() {
        ArgumentCaptor<AiKnowledgeDoc> docCap = ArgumentCaptor.forClass(AiKnowledgeDoc.class);
        verify(docMapper, org.mockito.Mockito.atLeastOnce()).updateById(docCap.capture());
        List<AiKnowledgeDoc> all = docCap.getAllValues();
        return all.get(all.size() - 1);
    }

    private AiKnowledgeDoc doc(Long id, String content) {
        AiKnowledgeDoc d = new AiKnowledgeDoc();
        d.setId(id);
        d.setContent(content);
        return d;
    }

    private List<double[]> vectors(int count) {
        List<double[]> list = new ArrayList<double[]>();
        for (int i = 0; i < count; i++) {
            list.add(new double[] {0.1d * (i + 1), 0.2d});
        }
        return list;
    }

    /** 每 50 字符一段、段内同字符的可定位文本（10 字符循环），长度 len */
    private static String patterned(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + (i / 50) % 10));
        }
        return sb.toString();
    }
}
