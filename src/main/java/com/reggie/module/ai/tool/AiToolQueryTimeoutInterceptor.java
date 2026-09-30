package com.reggie.module.ai.tool;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;

/**
 * AI 工具查询超时拦截器：仅对 {@link AiToolExecutor} worker 线程执行的 SQL
 * 设置 JDBC {@code queryTimeout}，慢查询在驱动层被打断、worker 线程得以回收。
 *
 * <p>背景：{@code future.cancel(true)} 的 interrupt 打不断阻塞在 JDBC socket 读上的线程，
 * 固定小线程池会被少数慢 SQL 永久占满，工具调用能力降级到重启才恢复。拦截器在
 * {@code StatementHandler.prepare} 返回的 Statement 上设置与工具 future 超时一致的
 * queryTimeout，两条超时路径（驱动层 SQLTimeoutException / future.get 超时）殊途同归。</p>
 *
 * <p>非工具线程（报表导出、后台长查询等）不设标记，完全不受影响；个别驱动或
 * 连接池包装不支持 setQueryTimeout 时静默跳过，退回仅靠 future 超时兜底。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare",
                args = {Connection.class, Integer.class})
})
public class AiToolQueryTimeoutInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object statement = invocation.proceed();
        if (AiToolExecutor.shouldApplyQueryTimeout() && statement instanceof Statement) {
            try {
                ((Statement) statement).setQueryTimeout(AiToolExecutor.TIMEOUT_SECONDS);
            } catch (Exception ignore) {
                // 驱动/包装层不支持：静默跳过，超时仍由 AiToolExecutor 的 future 兜底
            }
        }
        return statement;
    }
}
