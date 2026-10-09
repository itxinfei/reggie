package com.reggie.module.ai.adapter;

/**
 * AI 外部调用失败分类工具。
 * <p>
 * 背景：AI 供应商（OpenAI / DeepSeek / 通义千问 …）均为外部 HTTPS 服务。在离线、内网
 * 或防火墙受限的环境下调用必然失败，抛 {@link java.net.ConnectException}、
 * {@link java.net.UnknownHostException} 或 {@link java.net.SocketTimeoutException}。
 * 这类失败属于「运行环境/外部依赖」问题，并非应用缺陷；若一律按 ERROR + 全量堆栈输出，
 * 会周期性刷屏并淹没真正的程序缺陷。
 * <p>
 * 约定：外部网络类失败 → WARN（仅打印简要信息，不打堆栈）；其余异常 → ERROR（保留堆栈）。
 *
 * @author reggie
 * @since 2026-09-15
 */
public final class AiNetworkFailureUtils {

    private AiNetworkFailureUtils() {
        // 工具类不允许实例化
    }

    /**
     * 判断异常是否属于外部网络不可达/超时类失败。
     * <p>
     * 会沿 cause 链向上查找，以识别被 RuntimeException 包装的网络异常。
     *
     * @param t 待判断的异常，允许为 null
     * @return 属于网络类失败返回 true；否则返回 false
     */
    public static boolean isNetworkFailure(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof java.net.ConnectException
                || t instanceof java.net.UnknownHostException
                || t instanceof java.net.SocketTimeoutException
                || t instanceof java.net.NoRouteToHostException
                || t instanceof javax.net.ssl.SSLException) {
            return true;
        }
        Throwable cause = t.getCause();
        // 防止异常链自引用导致无限递归
        return cause != null && cause != t && isNetworkFailure(cause);
    }
}
