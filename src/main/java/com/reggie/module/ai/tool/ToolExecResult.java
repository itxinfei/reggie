package com.reggie.module.ai.tool;

import lombok.Data;

/**
 * 工具执行结果。
 * <p>{@code data} 序列化为 JSON 回灌模型；{@code summary} 是一句人话摘要，
 * 仅用于聊天气泡上的工具状态条展示，不发给模型。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
public class ToolExecResult {

    /** 是否执行成功（异常/超时为 false，仍会把失败原因回灌模型，避免协议断轮） */
    private boolean success;

    /** 状态条一句话摘要，如「近 7 天营业额 ¥12,345（128 单）」 */
    private String summary;

    /** 回灌模型的结构化数据（Map/List/字符串均可） */
    private Object data;

    public static ToolExecResult ok(String summary, Object data) {
        ToolExecResult r = new ToolExecResult();
        r.success = true;
        r.summary = summary;
        r.data = data;
        return r;
    }

    public static ToolExecResult fail(String summary, Object data) {
        ToolExecResult r = new ToolExecResult();
        r.success = false;
        r.summary = summary;
        r.data = data;
        return r;
    }
}
