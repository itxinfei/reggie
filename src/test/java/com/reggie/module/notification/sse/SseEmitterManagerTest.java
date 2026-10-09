package com.reggie.module.notification.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SseEmitterManager} 单元测试：验证按 userId 的连接注册、同用户多连接、
 * 离线静默、在线写入不抛，以及 completion/timeout 回调正确移除连接。
 * <p>
 * 未关联 Servlet 容器时，{@link SseEmitter} 的发送会缓存为早期事件，不抛异常；
 * 通过反射读取内部连接表与回调来观察状态。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-09-26
 */
class SseEmitterManagerTest {

    private SseEmitterManager manager;

    private Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        manager = new SseEmitterManager();
        // 纯单测下 @Value 不注入，手动打开开关、给出超时，便于心跳相关用例
        ReflectionTestUtils.setField(manager, "enabled", true);
        emitters = (Map<Long, CopyOnWriteArrayList<SseEmitter>>)
                ReflectionTestUtils.getField(manager, "emitters");
    }

    @Test
    void subscribe_应注册连接并返回Emitter() {
        SseEmitter emitter = manager.subscribe(1001L);
        assertNotNull(emitter);
        assertTrue(emitters.containsKey(1001L));
        assertEquals(1, emitters.get(1001L).size());
    }

    @Test
    void subscribe_同一用户多次_应保留多条连接() {
        manager.subscribe(1001L);
        manager.subscribe(1001L);
        assertEquals(2, emitters.get(1001L).size());
    }

    @Test
    void sendToUser_离线用户_应静默不抛() {
        assertDoesNotThrow(() -> manager.sendToUser(9999L, "message", new Payload("标题", "内容")));
        assertFalse(emitters.containsKey(9999L));
    }

    @Test
    void sendToUser_在线用户_应不抛异常() {
        manager.subscribe(1001L);
        assertDoesNotThrow(() -> manager.sendToUser(1001L, "message", new Payload("标题", "内容")));
        // 连接仍保留
        assertEquals(1, emitters.get(1001L).size());
    }

    @Test
    void broadcastHeartbeat_开启时_应不抛且保留连接() {
        manager.subscribe(1001L);
        assertDoesNotThrow(() -> manager.broadcastHeartbeat());
        assertTrue(emitters.containsKey(1001L));
    }

    @Test
    void completion回调触发_应移除连接() {
        SseEmitter emitter = manager.subscribe(1001L);
        Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
        assertNotNull(completionCallback);
        completionCallback.run();
        assertFalse(emitters.containsKey(1001L));
    }

    @Test
    void timeout回调触发_应移除连接() {
        SseEmitter emitter = manager.subscribe(1001L);
        Runnable timeoutCallback = (Runnable) ReflectionTestUtils.getField(emitter, "timeoutCallback");
        assertNotNull(timeoutCallback);
        timeoutCallback.run();
        assertFalse(emitters.containsKey(1001L));
    }

    /** 测试用推送载荷 */
    private static class Payload {
        private final String title;
        private final String content;

        Payload(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}
