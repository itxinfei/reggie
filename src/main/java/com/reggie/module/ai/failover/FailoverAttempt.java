package com.reggie.module.ai.failover;

/**
 * 一次候选尝试记录（用于全链路追踪、告警文案与状态展示）。
 *
 * @author reggie
 * @since 2026-09-26
 */
public class FailoverAttempt {

    /** 供应商编码 */
    private final String providerCode;

    /** 是否因熔断开启/探测繁忙被跳过（未真正发起调用） */
    private final boolean skipped;

    /** 失败类型（被跳过或成功时可能为 null） */
    private final AiFailureType failureType;

    /** 详情（跳过原因 / 异常消息） */
    private final String detail;

    public FailoverAttempt(String providerCode, boolean skipped, AiFailureType failureType, String detail) {
        this.providerCode = providerCode;
        this.skipped = skipped;
        this.failureType = failureType;
        this.detail = detail;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public AiFailureType getFailureType() {
        return failureType;
    }

    public String getDetail() {
        return detail;
    }
}
