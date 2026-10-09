package com.reggie.module.notification.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.ObjectMapperHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * C端用户 SSE 连接管理器。
 * <p>
 * 按 userId 维护在线的 {@link SseEmitter} 长连接（同一用户多标签页可有多条连接），
 * 供后台下发通知时实时推送到打开的 H5 页面；离线用户无连接时静默跳过，
 * 由已落库的站内信（marketing_message）在下次打开页面时补拉。
 * </p>
 * <p>
 * 纯内存组件、不依赖请求上下文，定时任务线程也可安全调用。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-09-26
 */
@Slf4j
@Component
public class SseEmitterManager {

    /** userId → 该用户的所有 SSE 连接 */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** JSON 序列化工具（复用全局 Holder，保证与全站口径一致） */
    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    /** SSE 总开关（关闭时停止心跳保活） */
    @Value("${reggie.sse.enabled:true}")
    private boolean enabled;

    /** 单条连接超时时间（毫秒） */
    @Value("${reggie.sse.timeout:1800000}")
    private long timeout;

    /**
     * 注册一个用户的 SSE 连接。
     *
     * @param userId C端用户ID
     * @return 交由 Spring MVC 挂起的 SseEmitter
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(timeout);
        CopyOnWriteArrayList<SseEmitter> list =
                emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<SseEmitter>());
        list.add(emitter);

        // 连接结束/超时/出错时务必从集合移除，避免内存泄漏与向死连接写数据
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(ex -> remove(userId, emitter));

        // 建链后立即下发 connected 事件，供前端确认通道可用
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"ok\"}"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        log.debug("[SSE] 用户订阅 userId={}, 当前连接数={}", userId, list.size());
        return emitter;
    }

    /**
     * 向某用户的所有在线连接推送事件；用户离线（无连接）时静默返回，不抛异常。
     *
     * @param userId    目标用户ID
     * @param eventName SSE 事件名
     * @param data      载荷（将被 JSON 序列化）
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("[SSE] 载荷序列化失败 userId={}", userId, e);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                // 单条连接写入失败（客户端已断开）即摘除并结束，不影响同用户其他连接
                list.remove(emitter);
                emitter.complete();
            }
        }
    }

    /**
     * 定时心跳：向所有连接下发 heartbeat，防止 Nginx/移动网关回收空闲连接，并剔除死连接。
     */
    @Scheduled(fixedDelayString = "${reggie.sse.heartbeat:30000}")
    public void broadcastHeartbeat() {
        if (!enabled) {
            return;
        }
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("{}"));
                } catch (Exception e) {
                    entry.getValue().remove(emitter);
                    emitter.complete();
                }
            }
        }
    }

    /**
     * 从集合中移除指定连接；若用户已无任何连接则移除整个键。
     */
    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId, list);
            }
        }
    }
}
