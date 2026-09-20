package com.reggie.module.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 工具调用过程事件（推送给前端 SSE tool 事件）。
 * <p>前端契约：{name, label, status, summary}，status ∈ running/done/error；
 * 同一次调用按 name 合并为状态条上的一条。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
@AllArgsConstructor
public class ToolEvent {

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_ERROR = "error";

    private String name;
    private String label;
    private String status;
    private String summary;

    public static ToolEvent running(String name, String label) {
        return new ToolEvent(name, label, STATUS_RUNNING, null);
    }

    public static ToolEvent done(String name, String label, String summary) {
        return new ToolEvent(name, label, STATUS_DONE, summary);
    }

    public static ToolEvent error(String name, String label, String summary) {
        return new ToolEvent(name, label, STATUS_ERROR, summary);
    }
}
