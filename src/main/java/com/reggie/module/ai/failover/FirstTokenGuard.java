package com.reggie.module.ai.failover;

import com.reggie.module.ai.adapter.AbortableStreamCallback;
import com.reggie.module.ai.adapter.AiModelAdapter.StreamCallback;

/**
 * 流式首 token 哨兵。
 *
 * <p>包装业务侧原始 StreamCallback，记录是否已向用户推送过实际内容：</p>
 * <ul>
 *   <li>首 token 推送<em>之前</em>供应商失败：内容尚未泄漏，故障转移可安全切换下一家；</li>
 *   <li>首 token 推送<em>之后</em>失败：再切换会造成内容重复/错乱，只能终止并提示。</li>
 * </ul>
 *
 * <p>本类恒实现 {@link AbortableStreamCallback}：被包装者支持中止时，中止语义原样委托；
 * 被包装者是普通 StreamCallback 时，{@link #isAborted()} 恒为 false，
 * 从而避免适配器因 instanceof 判断产生注册链路分叉。</p>
 *
 * @author reggie
 * @since 2026-09-26
 */
public class FirstTokenGuard implements AbortableStreamCallback {

    /** 被包装的业务回调 */
    private final StreamCallback delegate;

    /** 被包装者支持中止时非空，用于中止语义委托 */
    private final AbortableStreamCallback abortDelegate;

    /** 是否已推送过非空内容（volatile 保证异步线程可见性） */
    private volatile boolean started = false;

    public FirstTokenGuard(StreamCallback delegate) {
        this.delegate = delegate;
        this.abortDelegate = (delegate instanceof AbortableStreamCallback)
                ? (AbortableStreamCallback) delegate : null;
    }

    /**
     * 是否已向用户推送过首 token（非空内容）。
     */
    public boolean isStarted() {
        return started;
    }

    @Override
    public void onToken(String token, boolean isLast) {
        if (token != null && !token.isEmpty()) {
            started = true;
        }
        delegate.onToken(token, isLast);
    }

    @Override
    public boolean isAborted() {
        // 被包装者不支持中止时恒为 false
        return abortDelegate != null && abortDelegate.isAborted();
    }

    @Override
    public void registerAbortAction(Runnable action) {
        if (abortDelegate != null) {
            abortDelegate.registerAbortAction(action);
        }
        // 被包装者是普通 callback：无中止主体，适配器注册的断开动作无需转发
    }
}
