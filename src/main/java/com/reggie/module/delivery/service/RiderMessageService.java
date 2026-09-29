package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.RiderMessage;

/**
 * 骑手消息服务：消息写入（派单/催单/异常处理/公告）、骑手收件箱、未读数与已读标记。
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface RiderMessageService {

    /**
     * 写入一条骑手消息。
     *
     * @param riderId  接收骑手ID
     * @param tenantId 租户ID
     * @param type     消息类型
     * @param title    标题
     * @param content  内容
     * @param bizId    关联业务ID（订单ID，可空）
     * @return 消息
     */
    RiderMessage send(Long riderId, Long tenantId, Integer type, String title, String content, Long bizId);

    /**
     * 骑手收件箱（按创建时间倒序）。
     */
    Page<RiderMessage> pageMine(Long riderId, Long tenantId, int page, int size);

    /**
     * 当前骑手未读消息数。
     */
    long unreadCount(Long riderId, Long tenantId);

    /**
     * 标记单条已读（校验归属）。
     */
    RiderMessage markRead(Long id, Long riderId, Long tenantId);

    /**
     * 标记当前骑手全部已读，返回受影响条数。
     */
    int markAllRead(Long riderId, Long tenantId);

    /**
     * 系统公告：向租户下全部骑手广播一条消息。
     *
     * @param tenantId 租户ID
     * @param title    标题
     * @param content  内容
     * @return 发送条数
     */
    int broadcast(Long tenantId, String title, String content);
}
