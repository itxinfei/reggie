package com.reggie.module.ai.failover;

import com.reggie.module.ai.model.AiProviderConfig;

/**
 * 针对单个候选供应商的调用动作。
 *
 * @param <T> 返回类型
 * @author reggie
 * @since 2026-09-26
 */
@FunctionalInterface
public interface AiCallAction<T> {

    /**
     * 用指定候选供应商执行调用。
     *
     * @param candidate 候选供应商配置
     * @return 调用结果
     * @throws Exception 调用失败时抛出（期望为 {@link AiProviderException}）
     */
    T invoke(AiProviderConfig candidate) throws Exception;
}
