package com.reggie.module.notification.service.impl;

import com.reggie.module.marketing.mapper.MarketingMessageMapper;
import com.reggie.module.marketing.model.MarketingMessage;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.mapper.UserDeviceMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.model.UserDevice;
import com.reggie.module.notification.provider.PushProvider;
import com.reggie.module.notification.sse.SseEmitterManager;
import com.reggie.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationServiceImpl} 单元测试：聚焦后台「发APP推送到 C 端」的核心修复——
 * C 端从不注册设备（user_device 为空）时，消息仍应无条件落入消息中心并经 SSE 实时推送、
 * 计为成功；非法用户则不落库、计失败。
 *
 * @author 心飞为你飞
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRecordMapper recordMapper;

    @Mock
    private NotificationTemplateMapper templateMapper;

    @Mock
    private UserDeviceMapper userDeviceMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MarketingMessageMapper marketingMessageMapper;

    @Mock
    private PushProvider pushProvider;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    void sendSimpleMessage_app推送_无设备也应落站内信并实时推_计成功() {
        Long userId = 880001L;
        // 模拟 C 端从未注册设备：user_device 查询为空
        when(userDeviceMapper.selectList(any())).thenReturn(new ArrayList<UserDevice>());

        NotificationRecord record = service.sendSimpleMessage(
                2, Arrays.asList(String.valueOf(userId)), "优惠提醒", "您有一张新优惠券");

        assertNotNull(record);
        assertEquals(1, record.getSuccessCount());
        assertEquals(0, record.getFailCount());

        // 无条件落入消息中心（站内信兜底）
        verify(marketingMessageMapper).insert(any(MarketingMessage.class));
        // 对在线用户实时 SSE 推送
        verify(sseEmitterManager).sendToUser(eq(userId), eq("message"), any(MarketingMessage.class));
    }

    @Test
    void sendSimpleMessage_app推送_非法用户应计失败且不落库() {
        NotificationRecord record = service.sendSimpleMessage(
                2, Arrays.asList("not-a-user-id"), "标题", "内容");

        assertNotNull(record);
        assertEquals(0, record.getSuccessCount());
        assertEquals(1, record.getFailCount());

        // 无法解析出 userId：不落站内信、不推送
        verify(marketingMessageMapper, never()).insert(any(MarketingMessage.class));
        verify(sseEmitterManager, never()).sendToUser(any(), any(), any());
    }
}
