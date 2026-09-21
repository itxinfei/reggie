package com.reggie.module.ai.rag.service;

import java.util.List;

/**
 * Embedding 向量服务（P5 RAG）
 * <p>走当前激活供应商的 OpenAI 兼容 /embeddings 接口；
 * 未配置 embedding 能力时 {@link #isEmbeddingAvailable()} 为 false，链路自动降级。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface EmbeddingService {

    /**
     * 当前激活供应商是否开启 embedding 能力。
     * @return true 可调
     */
    boolean isEmbeddingAvailable();

    /**
     * 当前 embedding 模型名（取激活供应商 modelName），不可用时返回 null。
     * @return 模型名
     */
    String getEmbeddingModel();

    /**
     * 批量生成向量（单批 ≤16 条由调用方分批）。
     *
     * @param texts 文本列表
     * @return 与入参顺序一致的向量
     * @throws RuntimeException 调用失败（网络/HTTP 非 200/响应解析失败）
     */
    List<double[]> embed(List<String> texts);
}
