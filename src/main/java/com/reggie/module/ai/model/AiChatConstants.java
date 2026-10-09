package com.reggie.module.ai.model;

/**
 * AI 聊天模块常量：会话归属身份与消息状态。
 *
 * @author reggie
 * @since 2026-09-20
 */
public final class AiChatConstants {

    private AiChatConstants() {
    }

    /** 后台员工发起的会话 */
    public static final String ACTOR_EMPLOYEE = "EMPLOYEE";

    /** C 端用户发起的会话 */
    public static final String ACTOR_CUSTOMER = "CUSTOMER";

    /** 历史会话（actor_type 列上线前的数据） */
    public static final String ACTOR_UNKNOWN = "UNKNOWN";

    /** 消息状态：正常完成 */
    public static final String MSG_STATUS_COMPLETED = "completed";

    /** 消息状态：用户停止生成，已保留片段 */
    public static final String MSG_STATUS_STOPPED = "stopped";

    /** 消息状态：生成失败 */
    public static final String MSG_STATUS_FAILED = "failed";
}
