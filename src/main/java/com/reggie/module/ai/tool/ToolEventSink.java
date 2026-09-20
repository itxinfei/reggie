package com.reggie.module.ai.tool;

/**
 * 工具事件出口（由聊天会话实现，转为 SSE tool 事件推送）。
 *
 * @author reggie
 * @since 2026-09-20
 */
public interface ToolEventSink {

    /**
     * 推送一次工具状态变更。
     *
     * @param event 工具事件
     */
    void onToolEvent(ToolEvent event);
}
