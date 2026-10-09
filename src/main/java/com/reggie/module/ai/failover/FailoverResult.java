package com.reggie.module.ai.failover;

import java.util.List;

/**
 * 故障转移最终结果。
 *
 * @param <T> 成功时的值类型
 * @author reggie
 * @since 2026-09-26
 */
public class FailoverResult<T> {

    /** 结果形态 */
    public enum Outcome {
        /** 某个候选成功（可能经过若干次失败切换） */
        SUCCESS,
        /** 全部候选失败或被熔断跳过 */
        EXHAUSTED,
        /** 遇到不可重试错误（BAD_REQUEST），终止 */
        NON_RETRYABLE,
        /** 首 token 已发出后失败，不可切换 */
        POST_START_ERROR
    }

    private final Outcome outcome;
    private final T value;
    private final List<FailoverAttempt> attempts;

    private FailoverResult(Outcome outcome, T value, List<FailoverAttempt> attempts) {
        this.outcome = outcome;
        this.value = value;
        this.attempts = attempts;
    }

    public static <T> FailoverResult<T> ok(T value, List<FailoverAttempt> attempts) {
        return new FailoverResult<T>(Outcome.SUCCESS, value, attempts);
    }

    public static <T> FailoverResult<T> exhausted(List<FailoverAttempt> attempts) {
        return new FailoverResult<T>(Outcome.EXHAUSTED, null, attempts);
    }

    public static <T> FailoverResult<T> nonRetryable(List<FailoverAttempt> attempts) {
        return new FailoverResult<T>(Outcome.NON_RETRYABLE, null, attempts);
    }

    public static <T> FailoverResult<T> postStartError(List<FailoverAttempt> attempts) {
        return new FailoverResult<T>(Outcome.POST_START_ERROR, null, attempts);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public T getValue() {
        return value;
    }

    public List<FailoverAttempt> getAttempts() {
        return attempts;
    }
}
