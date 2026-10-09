package com.reggie.module.ai.failover;

import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.service.NotificationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 全供应商耗尽告警。
 *
 * <p>全部候选均不可用时，向 {@code notification_record} 落一条系统通知，
 * 管理员可在「消息通知记录」页看到；带防抖间隔，避免高并发时反复落库。
 * 不触发短信/推送，纯后台留痕。</p>
 *
 * @author reggie
 * @since 2026-09-26
 */
@Slf4j
@Component
public class AiExhaustedAlerter {

    /** 告警防抖间隔（毫秒） */
    private static final long DEBOUNCE_MS = 10 * 60_000L;

    /** 告警内容最大长度 */
    private static final int MAX_CONTENT = 900;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private NotificationRecordService recordService;

    private volatile long lastAlertTime = 0L;

    /**
     * 全部候选不可用时调用（内部按防抖间隔决定是否真正落库）。
     *
     * @param attempts 本次全部尝试记录
     */
    public synchronized void onExhausted(List<FailoverAttempt> attempts) {
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastAlertTime < DEBOUNCE_MS) {
            log.debug("[AI耗尽告警] 防抖期内，跳过落库");
            return;
        }

        NotificationRecord record = new NotificationRecord();
        // 全局告警不绑定具体租户（tenant_id 列允许 NULL）
        record.setBizType("AI_PROVIDER_EXHAUSTED");
        record.setChannel(2);
        record.setTargetType(1);
        record.setTargetValue("[]");
        record.setTargetCount(0);
        record.setContent(buildContent(attempts));
        record.setSendTime(LocalDateTime.now());
        // 系统记录无实际发送动作，按成功落库
        record.setStatus(2);
        record.setSuccessCount(1);
        record.setFailCount(0);
        try {
            recordService.save(record);
            lastAlertTime = nowMs;
            log.warn("[AI耗尽告警] 已落系统通知记录");
        } catch (Exception e) {
            // 告警落库失败不能影响主链路
            log.error("[AI耗尽告警] 落库失败", e);
        }
    }

    /**
     * 构造告警文案：逐个列出候选的失败类型 / 跳过原因。
     */
    private String buildContent(List<FailoverAttempt> attempts) {
        StringBuilder sb = new StringBuilder("【系统告警】AI 全部供应商不可用：\n");
        if (attempts.isEmpty()) {
            sb.append("（无候选记录）\n");
        } else {
            for (FailoverAttempt attempt : attempts) {
                sb.append(attempt.getProviderCode()).append('=');
                if (attempt.isSkipped()) {
                    sb.append("跳过(").append(attempt.getDetail()).append("); ");
                } else {
                    sb.append(attempt.getFailureType()).append(';');
                }
            }
            sb.append('\n');
        }
        sb.append("时间：").append(LocalDateTime.now().format(TIME_FMT));
        String content = sb.toString();
        return content.length() > MAX_CONTENT ? content.substring(0, MAX_CONTENT) : content;
    }
}
