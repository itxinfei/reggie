package com.reggie.module.notification.provider;

import com.reggie.module.notification.model.PushMessage;
import com.reggie.module.notification.model.UserDevice;

import java.util.List;

/**
 * 推送服务提供商统一接口（策略模式）
 * 所有推送平台（极光推送、个推、FCM等）需实现此接口
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface PushProvider {

    /**
     * 向单个设备发送推送
     *
     * @param device  用户设备信息（含deviceToken、platform）
     * @param message 结构化推送消息
     * @return true=发送成功
     */
    boolean pushToDevice(UserDevice device, PushMessage message);

    /**
     * 向一个用户的所有设备批量发送推送
     *
     * @param devices 该用户的所有设备列表
     * @param message 结构化推送消息
     * @return 成功发送的设备数
     */
    int pushToUserDevices(List<UserDevice> devices, PushMessage message);

    /**
     * 获取推送服务商名称
     */
    String getProviderName();

    /**
     * 是否处于Mock模式（仅打印日志不实际发送）
     */
    boolean isMockMode();
}
