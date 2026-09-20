package com.reggie.module.ai.adapter;

/**
 * 可中止的流式回调。
 * <p>用户点击「停止生成」时，服务层将 aborted 置 true 并执行适配器注册的
 * abortAction（断开上游 HttpURLConnection，打断阻塞中的 readLine）；
 * 适配器在读取循环中同时轮询 {@link #isAborted()}，尽快退出。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
public interface AbortableStreamCallback extends AiModelAdapter.StreamCallback {

    /**
     * 是否已被中止。
     *
     * @return true 表示适配器应尽快结束读取且不再推送错误事件
     */
    boolean isAborted();

    /**
     * 注册中止动作（由适配器在建立连接后调用，通常为 conn.disconnect()）。
     *
     * @param action 中止动作
     */
    void registerAbortAction(Runnable action);
}
