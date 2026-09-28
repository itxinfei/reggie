package com.reggie.module.ai.failover;

/**
 * AI 供应商调用失败类型。
 * <p>
 * 用于多厂家故障转移决策：{@link #retryable}=true 的失败（额度/限流/5xx/网络/鉴权/空响应）
 * 会自动切换下一个候选供应商；{@link #BAD_REQUEST} 属请求本身错误，换厂家无意义，不切换。
 * </p>
 *
 * @author reggie
 * @since 2026-09-26
 */
public enum AiFailureType {

    /** 额度/余额/计费问题（HTTP 402，或 429 响应体表明配额耗尽） */
    QUOTA(true, 402),

    /** 限流 / QPS 超限（HTTP 429，响应体无额度关键词） */
    RATE_LIMIT(true, 429),

    /** 上游 5xx 服务端错误，或响应无法解析 */
    SERVER_ERROR(true, 500),

    /** 网络问题：连接超时 / 读超时 / 连接失败 / DNS 解析失败 / SSL */
    NETWORK(true, 0),

    /** 鉴权失败：API key 无效 / 被禁用（HTTP 401 / 403） */
    AUTH_INVALID(true, 401),

    /** 请求参数错误（4xx 非鉴权类）：换厂家也无法解决，不切换 */
    BAD_REQUEST(false, 400),

    /** HTTP 200 但返回内容为空 */
    EMPTY(true, 200);

    /** 是否可通过切换供应商重试 */
    private final boolean retryable;

    /** 该类型典型的 HTTP 状态码（本地异常为 0） */
    private final int defaultHttpStatus;

    AiFailureType(boolean retryable, int defaultHttpStatus) {
        this.retryable = retryable;
        this.defaultHttpStatus = defaultHttpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getDefaultHttpStatus() {
        return defaultHttpStatus;
    }

    /**
     * 按 HTTP 状态码 + 错误响应体分类失败类型。
     *
     * @param httpStatus 上游返回的 HTTP 状态码
     * @param errorBody  错误响应体（可空），用于区分 429 是限流还是额度耗尽
     * @return 失败类型
     */
    public static AiFailureType fromHttp(int httpStatus, String errorBody) {
        if (httpStatus == 402) {
            return QUOTA;
        }
        if (httpStatus == 429) {
            // 429 可能是 QPS 限流，也可能是配额/余额耗尽（各厂家错误文案不同），按错误体区分
            return containsQuotaKeyword(errorBody) ? QUOTA : RATE_LIMIT;
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return AUTH_INVALID;
        }
        if (httpStatus >= 400 && httpStatus < 500) {
            return BAD_REQUEST;
        }
        // 5xx 及其他情况按服务端错误处理
        return SERVER_ERROR;
    }

    /**
     * 判断错误体是否包含额度/余额/计费类关键词（中英文）。
     */
    private static boolean containsQuotaKeyword(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) {
            return false;
        }
        String b = errorBody.toLowerCase();
        String[] keywords = {
                "quota", "insufficient", "balance", "credit", "billing",
                "exceeded your current", "account not active",
                "额度", "余额", "欠费", "配额"
        };
        for (String kw : keywords) {
            if (b.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
