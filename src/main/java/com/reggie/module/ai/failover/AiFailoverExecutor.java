package com.reggie.module.ai.failover;

import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.service.CircuitBreakerService;
import com.reggie.module.ai.service.CircuitBreakerService.BreakerDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 多供应商故障转移执行器。
 *
 * <p>按候选顺序（首选 → 备用）逐个尝试：熔断开启/探测繁忙的候选直接跳过；
 * 候选调用失败时按错误类型决定切换（QUOTA/RATE_LIMIT/SERVER_ERROR/NETWORK/
 * AUTH_INVALID/EMPTY 可切换，BAD_REQUEST 终止）；首 token 已发出后失败不可切换。
 * 全部候选均不可用时触发告警。</p>
 *
 * <p>首选正常时第一次循环即返回，请求长期黏在首选；首选冷却恢复探测成功后，
 * 下一个新请求自然回到首选（自动回切无需定时器）。</p>
 *
 * @author reggie
 * @since 2026-09-26
 */
@Slf4j
@Component
public class AiFailoverExecutor {

    @Resource
    private CircuitBreakerService circuitBreakerService;

    @Resource
    private AiExhaustedAlerter exhaustedAlerter;

    /** 最近一次结果是否为「全候选不可用」（供状态接口/后台横幅读取） */
    private volatile boolean lastExhausted = false;
    private volatile long lastExhaustedTime = 0L;
    private volatile List<FailoverAttempt> lastExhaustedAttempts = Collections.emptyList();

    /**
     * 执行故障转移调用。
     *
     * @param candidates 候选供应商（已排序）
     * @param action     针对单个候选的调用动作
     * @param <T>        返回类型
     * @return 最终结果
     */
    public <T> FailoverResult<T> execute(List<AiProviderConfig> candidates, AiCallAction<T> action) {
        List<FailoverAttempt> attempts = new ArrayList<FailoverAttempt>();

        for (AiProviderConfig candidate : candidates) {
            String code = candidate.getProviderCode();

            // 1) 熔断门禁：OPEN 冷却未到 / HALF_OPEN 探测槽占满 → 跳过该候选
            BreakerDecision decision = circuitBreakerService.beforeCall(code);
            if (decision == BreakerDecision.REJECT_OPEN || decision == BreakerDecision.REJECT_HALF_BUSY) {
                attempts.add(new FailoverAttempt(code, true, null, decision.name()));
                continue;
            }

            // 2) 调用（同一候选只打一次，失败立即切换，不在同家重试）
            try {
                T value = action.invoke(candidate);
                circuitBreakerService.recordSuccess(code);
                lastExhausted = false;
                log.info("AI候选[{}]调用成功（此前已尝试 {} 个候选）", code, attempts.size());
                return FailoverResult.ok(value, new ArrayList<FailoverAttempt>(attempts));
            } catch (AiProviderException e) {
                circuitBreakerService.recordFailure(code);
                attempts.add(new FailoverAttempt(code, false, e.getType(), e.getMessage()));

                // 2a) 首 token 已发出：再切换会重复内容，终止（不算全候选不可用，不亮横幅）
                if (e.isContentStarted()) {
                    log.warn("AI候选[{}]首token后中断，终止切换", code);
                    return FailoverResult.postStartError(new ArrayList<FailoverAttempt>(attempts));
                }
                log.warn("AI候选[{}]失败 type={} status={}，切换下一候选",
                        code, e.getType(), e.getHttpStatus());
                // 2b) 不可重试错误（请求本身问题）：终止，保留错误（不亮全故障横幅）
                if (!e.isRetryable()) {
                    return FailoverResult.nonRetryable(new ArrayList<FailoverAttempt>(attempts));
                }
                // 可重试失败 → 继续下一候选
            } catch (Exception e) {
                // 未被适配器包装的兜底异常：记一次失败并继续（极端防御）
                circuitBreakerService.recordFailure(code);
                log.error("AI候选[{}]未预期异常，切换下一候选", code, e);
                attempts.add(new FailoverAttempt(code, false, AiFailureType.SERVER_ERROR, e.getMessage()));
            }
        }

        // 3) 全部候选失败或被跳过：标记耗尽 + 触发告警（告警内部防抖）
        lastExhausted = true;
        lastExhaustedTime = System.currentTimeMillis();
        lastExhaustedAttempts = new ArrayList<FailoverAttempt>(attempts);
        exhaustedAlerter.onExhausted(attempts);
        return FailoverResult.exhausted(new ArrayList<FailoverAttempt>(attempts));
    }

    public boolean isLastOutcomeExhausted() {
        return lastExhausted;
    }

    public long getLastExhaustedTime() {
        return lastExhaustedTime;
    }

    public List<FailoverAttempt> getLastExhaustedAttempts() {
        return new ArrayList<FailoverAttempt>(lastExhaustedAttempts);
    }
}
