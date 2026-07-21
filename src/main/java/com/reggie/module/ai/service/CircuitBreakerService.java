package com.reggie.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 服务熔断与降级管理
 *
 * <p>核心能力：
 * <ul>
 *   <li>滑动窗口熔断器：统计错误率，错误率超阈值自动熔断</li>
 *   <li>熔断恢复：冷却期后逐步放行探测</li>
 *   <li>降级策略：熔断时返回友好提示，引导用户重试</li>
 *   <li>追问机制：服务异常时引导用户提供更多信息便于恢复后重试</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
public class CircuitBreakerService {

    /** 统计窗口大小（请求数） */
    private static final int WINDOW_SIZE = 10;

    /** 错误率阈值（超过此比例触发熔断） */
    private static final float ERROR_THRESHOLD = 0.5f;

    /** 熔断冷却期（毫秒） */
    private static final long COOLDOWN_MS = 30_000L;

    /** 探测期每次放行数量 */
    private static final int PROBE_COUNT = 3;

    /** 按供应商分组的熔断状态 */
    private final Map<String, BreakerState> states = new ConcurrentHashMap<>();

    /** 全局统计 */
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailure = new AtomicLong(0);

    // ==================== 公共 API ====================

    /**
     * 执行 AI 请求（带熔断保护）
     *
     * @param providerCode 供应商编码
     * @param supplier     实际执行的逻辑
     * @param fallback     降级逻辑（熔断时执行）
     * @return 执行结果
     */
    public <T> T execute(String providerCode, SupplierWithException<T> supplier, Fallback<T> fallback) {
        if (providerCode == null || providerCode.isEmpty()) {
            providerCode = "default";
        }
        BreakerState state = states.computeIfAbsent(providerCode, k -> new BreakerState());

        // 熔断开启状态
        if (state.isOpen()) {
            // 检查是否进入冷却期
            if (System.currentTimeMillis() - state.openTime > COOLDOWN_MS) {
                state.transitionToHalfOpen();
                log.info("熔断器进入半开探测期: provider={}", providerCode);
            } else {
                log.debug("请求被熔断拒绝: provider={}, 剩余冷却: {}s",
                        providerCode, (COOLDOWN_MS - (System.currentTimeMillis() - state.openTime)) / 1000);
                return fallback.apply(providerCode, "服务暂时不可用，请稍后重试");
            }
        }

        // 半开状态：限制探测数量
        if (state.isHalfOpen()) {
            if (state.probeCount.getAndIncrement() >= PROBE_COUNT) {
                log.debug("半开探测排队: provider={}", providerCode);
                return fallback.apply(providerCode, "服务恢复中，请稍后重试");
            }
        }

        // 执行请求
        try {
            T result = supplier.get();
            onSuccess(providerCode, state);
            return result;
        } catch (Exception e) {
            onFailure(providerCode, state);
            return fallback.apply(providerCode, e.getMessage());
        }
    }

    /**
     * 获取供应商熔断状态
     */
    public String getStatus(String providerCode) {
        if (providerCode == null) return "unknown";
        BreakerState state = states.get(providerCode);
        if (state == null) return "closed";
        if (state.isOpen()) return "open (cooldown)";
        if (state.isHalfOpen()) return "half-open (probing)";
        return "closed";
    }

    /**
     * 获取全局统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalSuccess", totalSuccess.get());
        stats.put("totalFailure", totalFailure.get());
        stats.put("providers", states.keySet().size());
        return stats;
    }

    /**
     * 手动重置熔断器（管理员操作）
     */
    public void reset(String providerCode) {
        if (providerCode != null) {
            states.remove(providerCode);
            log.info("熔断器已手动重置: provider={}", providerCode);
        }
    }

    // ==================== 内部状态流转 ====================

    private void onSuccess(String providerCode, BreakerState state) {
        totalSuccess.incrementAndGet();
        state.recordSuccess();
        if (state.isHalfOpen() && state.successCount >= PROBE_COUNT) {
            state.transitionToClosed();
            log.info("熔断器恢复关闭: provider={}", providerCode);
        }
    }

    private void onFailure(String providerCode, BreakerState state) {
        totalFailure.incrementAndGet();
        state.recordFailure();
        if (state.shouldOpen()) {
            state.transitionToOpen();
            log.warn("熔断器触发开启: provider={}, errorRate={}",
                    providerCode, state.getErrorRate());
        }
    }

    // ==================== 内部类 ====================

    /**
     * 单个供应商的熔断状态
     */
    private static class BreakerState {
        /** 滑动窗口（最近 WINDOW_SIZE 次结果） */
        private final AtomicInteger[] window = new AtomicInteger[WINDOW_SIZE];
        private int windowIndex = 0;
        private int totalInWindow = 0;
        private int failureInWindow = 0;

        /** 熔断状态 */
        enum Status { CLOSED, OPEN, HALF_OPEN }
        private volatile Status status = Status.CLOSED;

        /** 熔断开启时间 */
        private volatile long openTime = 0L;

        /** 半开探测计数 */
        private final AtomicInteger probeCount = new AtomicInteger(0);

        /** 连续成功计数 */
        private int successCount = 0;

        BreakerState() {
            for (int i = 0; i < WINDOW_SIZE; i++) {
                window[i] = new AtomicInteger(0); // 0 = 未使用, 1 = 成功, -1 = 失败
            }
        }

        synchronized void recordSuccess() {
            pushResult(1);
            failureInWindow = Math.max(0, failureInWindow - 1);
            if (status == Status.HALF_OPEN) {
                successCount++;
            }
        }

        synchronized void recordFailure() {
            pushResult(-1);
            failureInWindow++;
            if (status == Status.HALF_OPEN) {
                successCount = 0;
            }
        }

        synchronized boolean shouldOpen() {
            if (totalInWindow < WINDOW_SIZE) return false;
            return (float) failureInWindow / totalInWindow > ERROR_THRESHOLD;
        }

        synchronized float getErrorRate() {
            if (totalInWindow == 0) return 0f;
            return (float) failureInWindow / totalInWindow;
        }

        boolean isOpen() { return status == Status.OPEN; }
        boolean isHalfOpen() { return status == Status.HALF_OPEN; }

        void transitionToOpen() {
            status = Status.OPEN;
            openTime = System.currentTimeMillis();
            probeCount.set(0);
        }

        void transitionToHalfOpen() {
            status = Status.HALF_OPEN;
            probeCount.set(0);
            successCount = 0;
        }

        void transitionToClosed() {
            status = Status.CLOSED;
            windowIndex = 0;
            totalInWindow = 0;
            failureInWindow = 0;
        }

        private void pushResult(int result) {
            int old = window[windowIndex].getAndSet(result);
            if (old == -1) failureInWindow = Math.max(0, failureInWindow - 1);
            if (old == 1) totalInWindow = Math.max(0, totalInWindow - 1);

            if (result == -1) failureInWindow++;
            totalInWindow = Math.min(WINDOW_SIZE, totalInWindow + 1);

            windowIndex = (windowIndex + 1) % WINDOW_SIZE;
        }
    }

    // ==================== 函数式接口 ====================

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface Fallback<T> {
        T apply(String providerCode, String reason);
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        log.info("熔断降级服务初始化完成: windowSize={}, errorThreshold={}, cooldown={}s",
                WINDOW_SIZE, ERROR_THRESHOLD, COOLDOWN_MS / 1000);
    }
}
