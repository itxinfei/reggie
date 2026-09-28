package com.reggie.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 服务熔断管理。
 *
 * <p>核心能力：
 * <ul>
 *   <li>滑动窗口熔断器：统计错误率，错误率超阈值自动熔断</li>
 *   <li>熔断恢复：冷却期后进入半开，放少量探测请求，一次探测成功即恢复关闭</li>
 *   <li>细粒度门禁 API（{@link #beforeCall} / {@link #recordSuccess} /
 *       {@link #recordFailure}），供多厂家故障转移执行器逐个候选协作</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
public class CircuitBreakerService {

    /** 默认统计窗口大小（请求数） */
    static final int DEFAULT_WINDOW_SIZE = 10;

    /** 默认错误率阈值（超过此比例触发熔断） */
    static final float DEFAULT_ERROR_THRESHOLD = 0.5f;

    /** 默认熔断冷却期（毫秒） */
    static final long DEFAULT_COOLDOWN_MS = 30_000L;

    /** 默认半开并发探测上限 */
    static final int DEFAULT_PROBE_LIMIT = 3;

    /** 调用前门禁决策 */
    public enum BreakerDecision {
        /** CLOSED 正常放行 */
        ALLOW,
        /** HALF_OPEN 探测放行 */
        ALLOW_PROBE,
        /** OPEN 冷却未到，拒绝 */
        REJECT_OPEN,
        /** HALF_OPEN 探测槽占满，拒绝 */
        REJECT_HALF_BUSY
    }

    /** 熔断状态快照（后台状态展示） */
    public static class BreakerSnapshot {
        private final String state;
        private final float errorRate;
        private final long cooldownRemainingMs;

        BreakerSnapshot(String state, float errorRate, long cooldownRemainingMs) {
            this.state = state;
            this.errorRate = errorRate;
            this.cooldownRemainingMs = cooldownRemainingMs;
        }

        public String getState() { return state; }
        public float getErrorRate() { return errorRate; }
        public long getCooldownRemainingMs() { return cooldownRemainingMs; }
    }

    private final int windowSize;
    private final float errorThreshold;
    private final long cooldownMs;
    private final int probeLimit;

    /** 按供应商分组的熔断状态 */
    private final Map<String, BreakerState> states = new ConcurrentHashMap<>();

    /** 全局统计 */
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailure = new AtomicLong(0);

    /** 生产环境使用默认参数 */
    public CircuitBreakerService() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_ERROR_THRESHOLD, DEFAULT_COOLDOWN_MS, DEFAULT_PROBE_LIMIT);
    }

    /** 测试可定制参数（如缩短冷却期验证恢复） */
    CircuitBreakerService(int windowSize, float errorThreshold, long cooldownMs, int probeLimit) {
        this.windowSize = windowSize;
        this.errorThreshold = errorThreshold;
        this.cooldownMs = cooldownMs;
        this.probeLimit = probeLimit;
    }

    // ==================== 细粒度 API ====================

    /**
     * 调用前门禁。OPEN 冷却到期时在此原子地转 HALF_OPEN。
     */
    public BreakerDecision beforeCall(String providerCode) {
        return state(providerCode).admit();
    }

    /**
     * 记录一次调用成功。HALF_OPEN 时一次成功即恢复 CLOSED。
     */
    public void recordSuccess(String providerCode) {
        totalSuccess.incrementAndGet();
        state(providerCode).recordSuccess();
    }

    /**
     * 记录一次调用失败。HALF_OPEN 时一次失败即重新 OPEN；CLOSED 时按错误率判断。
     */
    public void recordFailure(String providerCode) {
        totalFailure.incrementAndGet();
        state(providerCode).recordFailure();
    }

    /**
     * 获取供应商熔断状态快照（state / errorRate / 冷却剩余）。
     */
    public BreakerSnapshot describe(String providerCode) {
        BreakerState s = providerCode == null ? null : states.get(providerCode);
        return s == null ? new BreakerSnapshot("closed", 0f, 0L) : s.snapshot();
    }

    /**
     * 获取供应商熔断状态
     */
    public String getStatus(String providerCode) {
        if (providerCode == null) {
            return "unknown";
        }
        BreakerState s = states.get(providerCode);
        return s == null ? "closed" : s.snapshot().getState();
    }

    /**
     * 获取全局统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalSuccess", totalSuccess.get());
        stats.put("totalFailure", totalFailure.get());
        stats.put("providers", states.size());
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

    private BreakerState state(String providerCode) {
        String key = (providerCode == null || providerCode.isEmpty()) ? "default" : providerCode;
        return states.computeIfAbsent(key,
                k -> new BreakerState(windowSize, errorThreshold, cooldownMs, probeLimit));
    }

    // ==================== 内部类 ====================

    /**
     * 单个供应商的熔断状态。非静态内部类，直接持有外部熔断参数。
     */
    private static class BreakerState {
        /** 熔断参数（由外部服务通过构造器传入） */
        private final int windowSize;
        private final float errorThreshold;
        private final long cooldownMs;
        private final int probeLimit;

        /** 滑动窗口（0=未使用, 1=成功, -1=失败） */
        private final int[] window;
        private int windowIndex = 0;
        private int totalInWindow = 0;
        private int failureInWindow = 0;

        private Status status = Status.CLOSED;
        private long openTime = 0L;

        /** 在途半开探测数 */
        int probeInFlight = 0;

        BreakerState(int windowSize, float errorThreshold, long cooldownMs, int probeLimit) {
            this.windowSize = windowSize;
            this.errorThreshold = errorThreshold;
            this.cooldownMs = cooldownMs;
            this.probeLimit = probeLimit;
            this.window = new int[windowSize];
        }

        /**
         * 门禁判定。状态迁移与探测槽占用在同一 synchronized 临界区完成，
         * 防止并发请求同时触发 OPEN→HALF_OPEN 或超额放行。
         */
        synchronized BreakerDecision admit() {
            if (status == Status.CLOSED) {
                return BreakerDecision.ALLOW;
            }
            if (status == Status.OPEN) {
                if (System.currentTimeMillis() - openTime >= cooldownMs) {
                    status = Status.HALF_OPEN;
                    probeInFlight = 1;
                    log.info("熔断器进入半开探测期");
                    return BreakerDecision.ALLOW_PROBE;
                }
                return BreakerDecision.REJECT_OPEN;
            }
            // HALF_OPEN
            if (probeInFlight < probeLimit) {
                probeInFlight++;
                return BreakerDecision.ALLOW_PROBE;
            }
            return BreakerDecision.REJECT_HALF_BUSY;
        }

        synchronized void recordSuccess() {
            if (status == Status.HALF_OPEN) {
                transitionToClosed();
                log.info("探测成功，熔断器恢复关闭");
                return;
            }
            pushResult(1);
        }

        synchronized void recordFailure() {
            if (status == Status.HALF_OPEN) {
                transitionToOpen();
                log.warn("探测失败，熔断器重新开启");
                return;
            }
            pushResult(-1);
            if (shouldOpen()) {
                transitionToOpen();
                log.warn("熔断器触发开启，errorRate={}", getErrorRate());
            }
        }

        private boolean shouldOpen() {
            if (totalInWindow < windowSize) {
                return false;
            }
            return (float) failureInWindow / totalInWindow > errorThreshold;
        }

        private float getErrorRate() {
            return totalInWindow == 0 ? 0f : (float) failureInWindow / totalInWindow;
        }

        /** 推入一次结果并更新滑动窗口统计 */
        private void pushResult(int result) {
            int old = window[windowIndex];
            if (old == -1) {
                failureInWindow--;
            }
            if (old != 0) {
                totalInWindow--;
            }
            window[windowIndex] = result;
            if (result == -1) {
                failureInWindow++;
            }
            totalInWindow++;
            windowIndex = (windowIndex + 1) % windowSize;
        }

        private void transitionToOpen() {
            status = Status.OPEN;
            openTime = System.currentTimeMillis();
            probeInFlight = 0;
        }

        private void transitionToClosed() {
            status = Status.CLOSED;
            Arrays.fill(window, 0);
            windowIndex = 0;
            totalInWindow = 0;
            failureInWindow = 0;
            probeInFlight = 0;
        }

        synchronized BreakerSnapshot snapshot() {
            long remaining = 0L;
            if (status == Status.OPEN) {
                remaining = Math.max(0L, cooldownMs - (System.currentTimeMillis() - openTime));
            }
            return new BreakerSnapshot(status.name().toLowerCase().replace('_', '-'),
                    getErrorRate(), remaining);
        }

        enum Status { CLOSED, OPEN, HALF_OPEN }
    }
}
