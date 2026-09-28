package com.reggie.module.ai.failover;

import com.reggie.module.ai.adapter.AiNetworkFailureUtils;
import com.reggie.module.ai.model.AiProviderConfig;

/**
 * AI 供应商调用异常。
 * <p>
 * 适配器在调用上游失败时统一抛出本异常（取代历史「吞成错误 AIChatResponse」的做法），
 * 携带分类后的 {@link AiFailureType}，供故障转移执行器决定是否切换下一个供应商。
 * </p>
 * <p>
 * {@link #contentStarted}=true 表示流式输出已向用户推送过首 token，此后失败不能切换
 * （会造成内容重复），只能终止。
 * </p>
 *
 * @author reggie
 * @since 2026-09-26
 */
public class AiProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误消息最大长度，防止上游返回超长错误体 */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /** 失败类型 */
    private final AiFailureType type;

    /** 上游 HTTP 状态码（本地异常为 0） */
    private final int httpStatus;

    /** 流式首 token 是否已发出：true 时不可切换 */
    private final boolean contentStarted;

    public AiProviderException(AiFailureType type, int httpStatus, String message) {
        this(type, httpStatus, message, null, false);
    }

    public AiProviderException(AiFailureType type, int httpStatus, String message, Throwable cause) {
        this(type, httpStatus, message, cause, false);
    }

    private AiProviderException(AiFailureType type, int httpStatus, String message,
                                Throwable cause, boolean contentStarted) {
        super(truncate(message), cause);
        this.type = type;
        this.httpStatus = httpStatus;
        this.contentStarted = contentStarted;
    }

    public AiFailureType getType() {
        return type;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isContentStarted() {
        return contentStarted;
    }

    public boolean isRetryable() {
        return type.isRetryable();
    }

    /**
     * 上游返回非 200：按状态码 + 错误体分类。
     *
     * @param config       被调用的供应商配置
     * @param httpStatus   上游 HTTP 状态码
     * @param errorBody    错误响应体
     * @return 分类异常
     */
    public static AiProviderException upstream(AiProviderConfig config, int httpStatus, String errorBody) {
        AiFailureType failureType = AiFailureType.fromHttp(httpStatus, errorBody);
        String message = "供应商「" + safeName(config) + "」请求失败（HTTP " + httpStatus + "）"
                + (errorBody == null ? "" : ": " + errorBody);
        return new AiProviderException(failureType, httpStatus, message);
    }

    /**
     * HTTP 200 但返回空内容。
     */
    public static AiProviderException empty(AiProviderConfig config) {
        return new AiProviderException(AiFailureType.EMPTY, 200,
                "供应商「" + safeName(config) + "」返回空内容");
    }

    /**
     * 本地异常（网络 / IO / 解析失败）分类。
     *
     * @param config 被调用的供应商配置
     * @param cause  原始异常
     * @return 分类异常
     */
    public static AiProviderException local(AiProviderConfig config, Throwable cause) {
        String msg = cause == null ? "调用失败" : cause.getMessage();
        if (cause instanceof java.net.SocketTimeoutException || AiNetworkFailureUtils.isNetworkFailure(cause)) {
            return new AiProviderException(AiFailureType.NETWORK, 0,
                    "供应商「" + safeName(config) + "」网络异常: " + msg, cause);
        }
        return new AiProviderException(AiFailureType.SERVER_ERROR, 0,
                "供应商「" + safeName(config) + "」调用异常: " + msg, cause);
    }

    /**
     * 流式首 token 发出后的中途失败：终止性异常，不可切换。
     */
    public static AiProviderException afterStarted(Throwable cause) {
        String msg = cause == null ? "流式输出中断" : cause.getMessage();
        return new AiProviderException(AiFailureType.SERVER_ERROR, 0,
                "AI回答生成中断: " + msg, cause, true);
    }

    private static String safeName(AiProviderConfig config) {
        if (config == null || config.getProviderName() == null) {
            return "未知";
        }
        return config.getProviderName();
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_MESSAGE_LENGTH ? text : text.substring(0, MAX_MESSAGE_LENGTH);
    }
}
