package com.reggie.module.notification.service;

import com.reggie.module.notification.model.NotificationRecord;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 消息通知服务接口
 * </p>
 * <p>统一管理短信发送与APP推送</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface NotificationService {

    /**
     * 根据业务类型和参数发送通知（自动匹配模板）
     *
     * @param bizType  业务类型，如 ORDER_NOTICE/PROMOTION/VERIFY_CODE
     * @param targets  目标列表（手机号或用户ID列表）
     * @param channel  发送渠道: 1=短信, 2=APP推送
     * @param params   模板参数 Map，如 {"userName": "张三", "orderNo": "NO123"}
     * @return 通知记录
     */
    NotificationRecord sendByBizType(String bizType, List<String> targets, Integer channel,
                                     Map<String, String> params);

    /**
     * 批量发送通知（支持定时发送）
     *
     * @param templateId 模板ID
     * @param targets    目标列表
     * @param channel    渠道
     * @param targetType 目标类型: 1=单个用户, 2=用户分组, 3=全部用户
     * @param params     模板参数
     * @param sendTime   定时发送时间，null表示立即发送
     * @return 通知记录
     */
    NotificationRecord batchSend(Long templateId, List<String> targets, Integer channel,
                                 Integer targetType, Map<String, String> params,
                                 java.time.LocalDateTime sendTime);

    /**
     * 修改点：向全部用户发送通知
     * 自动查询所有状态正常的用户，按渠道构建目标列表后发送
     *
     * @param bizType 业务类型，如 ORDER_NOTICE/PROMOTION/VERIFY_CODE
     * @param channel 发送渠道: 1=短信, 2=APP推送, 3=短信+APP推送
     * @param params  模板参数 Map
     * @return 通知发送记录
     */
    NotificationRecord sendToAllUsers(String bizType, Integer channel, Map<String, String> params);

    /**
     * 发送单条短信
     *
     * @param phone    手机号
     * @param signName 签名
     * @param templateCode 模板编码
     * @param params   模板参数JSON
     * @return true=成功
     */
    boolean sendSms(String phone, String signName, String templateCode, String params);

    /**
     * 模拟APP推送（生产环境对接极光/个推等SDK）
     *
     * @param userIds  用户ID列表
     * @param title    推送标题
     * @param content  推送内容
     * @return 成功推送的设备数
     */
    int sendAppPush(List<Long> userIds, String title, String content);

    /**
     * 注册/更新用户设备Token
     *
     * @param userId      用户ID
     * @param platform    平台 ANDROID/IOS/H5
     * @param deviceToken 设备Token
     */
    void registerDevice(Long userId, String platform, String deviceToken);

    /**
     * 修改点：简易消息发送（无需模板，直接发送文本内容）
     *
     * @param channel 发送渠道: 1=短信, 2=APP推送
     * @param targets 目标列表（手机号或用户ID）
     * @param title   消息标题（推送用，短信可null）
     * @param content 消息内容
     * @return 发送记录
     */
    NotificationRecord sendSimpleMessage(Integer channel, List<String> targets, String title, String content);

    /**
     * 修改点：获取当前是否为Mock模式
     */
    boolean isMockMode();
}
