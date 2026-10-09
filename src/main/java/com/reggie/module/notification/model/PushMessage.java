package com.reggie.module.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 推送消息模型（结构化推送载荷）
 * 包含标题、内容、点击行为及扩展数据，供各推送平台统一使用
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推送标题（必填，显示在通知栏） */
    private String title;

    /** 推送内容（必填，通知栏正文） */
    private String content;

    /** 点击后的跳转动作: APP_PAGE=应用内页面, URL=外部链接, NONE=无跳转 */
    private String clickAction;

    /** 点击动作对应的URI（如 /pages/order/detail 或 https://...） */
    private String clickUri;

    /** 徽章数字（iOS专属，设置角标数量，0=清除角标） */
    private Integer badge;

    /** 通知提示音（Android可设为raw资源名，iOS为default） */
    private String sound;

    /** 扩展数据（透传给客户端，如 orderId、couponId 等业务参数） */
    private Map<String, String> extras;
}
