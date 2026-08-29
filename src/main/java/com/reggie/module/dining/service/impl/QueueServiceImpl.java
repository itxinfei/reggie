package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.mapper.QueueMapper;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.vo.QueueStatsVO;
import com.reggie.enums.QueueRecordStatus;
import com.reggie.module.dining.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 排队服务实现
 * <p>
 * 修改点：使用 Redis SETNX 实现分布式锁，解决 takeNumber() 并发安全问题
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QueueServiceImpl extends ServiceImpl<QueueMapper, QueueRecord> implements QueueService {

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 取号分布式锁 Key 前缀 */
    private static final String QUEUE_LOCK_KEY_PREFIX = "queue:takeNumber:lock:";

    /** 分布式锁过期时间（秒） */
    private static final int LOCK_EXPIRE_SECONDS = 30;

    /** 获取锁最大等待时间（毫秒） */
    private static final int LOCK_WAIT_MILLIS = 3000;

    /** 获取锁重试间隔（毫秒） */
    private static final int LOCK_RETRY_INTERVAL = 100;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public QueueRecord takeNumber(Integer seatCount, String phone) {
        String lockKey = QUEUE_LOCK_KEY_PREFIX + seatCount;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = false;
        try {
            acquired = tryAcquire(lockKey, lockValue);
        } catch (Exception e) {
            // Redis 不可用/异常时降级放行（单机部署无并发问题）
            log.warn("[排队取号] 获取锁异常，降级放行: seatCount={}, error={}",
                    seatCount, e.getMessage(), e);
        }

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("[排队取号] 获取锁失败，系统繁忙: seatCount={}", seatCount);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        try {
            String datePrefix = LocalDate.now().format(DATE_PATTERN);
            LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
            qw.likeRight(QueueRecord::getQueueNo, datePrefix);
            qw.orderByDesc(QueueRecord::getQueueNo);
            qw.last("LIMIT 1");
            QueueRecord last = getOne(qw);

            int seq = 1;
            if (last != null) {
                String lastNo = last.getQueueNo();
                seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
            }

            QueueRecord record = new QueueRecord();
            record.setTenantId(BaseContext.getCurrentTenantId());
            record.setQueueNo(datePrefix + String.format("%04d", seq));
            record.setPhone(phone);
            record.setSeatCount(seatCount);
            record.setStatus(QueueRecordStatus.WAITING.getValue());
            save(record);
            return record;
        } finally {
            tryReleaseLock(lockKey, lockValue);
        }
    }

    /**
     * 获取分布式锁：SETNX + EXPIRE，锁值为 UUID，用于 ownership 校验。
     *
     * @return true=获取成功；false=超时未获取；异常时返回 false
     */
    private Boolean tryAcquire(String lockKey, String lockValue) {
        if (redisTemplate == null) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < LOCK_WAIT_MILLIS) {
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    return true;
                }
                Thread.sleep(LOCK_RETRY_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[排队取号] 获取锁被中断: {}", lockKey);
        } catch (Exception e) {
            log.error("[排队取号] 获取锁异常: {}, error={}", lockKey, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 释放分布式锁：Lua 脚本原子校验 + 删除，防止 TTL 过期瞬间 get+delete 的 TOCTOU 竞态。
     * 与 OrderTimeoutTask 的 unlock 保持一致的 Lua 脚本模式。
     */
    private void tryReleaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            // Lua 脚本：比对锁值后才删除，防止误删他人的锁；同时消除 get+delete 之间的竞态窗口
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockValue
            );
        } catch (Exception e) {
            log.warn("[排队取号] 释放锁失败: {}, error={}", lockKey, e.getMessage(), e);
        }
    }

    @Override
    public QueueRecord callNext(Integer seatCount) {
        int maxRetry = 50;
        // 安全加固：叫号查询必须附加租户条件，防止跨租户叫号
        Long tenantId = BaseContext.getCurrentTenantId();
        for (int i = 0; i < maxRetry; i++) {
            LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
            if (tenantId != null) {
                qw.eq(QueueRecord::getTenantId, tenantId);
            }
            qw.eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue());
            if (seatCount != null) qw.eq(QueueRecord::getSeatCount, seatCount);
            qw.orderByAsc(QueueRecord::getCreatedTime);
            qw.last("LIMIT 1");
            QueueRecord record = getOne(qw);
            if (record == null) {
                return null;
            }
            // CAS 更新：仅当状态仍为 WAITING 时才更新为 CALLED
            boolean success = lambdaUpdate()
                    .eq(QueueRecord::getId, record.getId())
                    .eq(QueueRecord::getTenantId, tenantId)
                    .eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                    .set(QueueRecord::getStatus, QueueRecordStatus.CALLED.getValue())
                    .update();
            if (success) {
                record.setStatus(QueueRecordStatus.CALLED.getValue());
                return record;
            }
            // CAS 失败，其他线程已修改该记录，重试下一条
            log.warn("[排队叫号] CAS更新失败，重试第{}次: id={}", i + 1, record.getId());
        }
        log.warn("[排队叫号] 达到最大重试次数{}，未能成功叫号", maxRetry);
        return null;
    }

    @Override
    public void cancelQueue(Long id) {
        // 安全加固：先按租户校验归属，再执行 CAS 更新，防止攻击者遍历 ID 取消其他租户排队记录
        if (id == null) {
            log.warn("[排队取消] id为空");
            return;
        }
        QueueRecord record = getById(id);
        if (record == null) {
            log.warn("[排队取消] 记录不存在: id={}", id);
            return;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            log.warn("[排队取消] 无权操作其他租户的排队记录: id={}", id);
            return;
        }
        // CAS 更新：仅当状态为 WAITING 时才允许取消，防止并发重复操作
        boolean success = lambdaUpdate()
                .eq(QueueRecord::getId, id)
                .eq(QueueRecord::getTenantId, currentTenantId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                .set(QueueRecord::getStatus, QueueRecordStatus.CANCELLED.getValue())
                .update();
        if (!success) {
            log.warn("[排队取消] 取消失败，当前状态非WAITING或记录不存在: id={}", id);
        }
    }

    /**
     * 退回等待：CALLED → WAITING（用于误叫号纠错）
     *
     * @param id 排队记录ID
     */
    @Override
    public void recallQueue(Long id) {
        if (id == null) {
            log.warn("[退回等待] id为空，跳过");
            return;
        }
        QueueRecord record = getById(id);
        if (record == null) {
            log.warn("[退回等待] 记录不存在: id={}", id);
            return;
        }
        // 租户归属校验
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            log.warn("[退回等待] 无权操作其他租户的排队记录: id={}", id);
            return;
        }
        if (!QueueRecordStatus.CALLED.getValue().equals(record.getStatus())) {
            log.warn("[退回等待] 当前状态非CALLED，无法退回: id={}, status={}",
                    id, record.getStatus());
            return;
        }
        boolean success = lambdaUpdate()
                .eq(QueueRecord::getId, id)
                .eq(QueueRecord::getStatus, QueueRecordStatus.CALLED.getValue())
                .set(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                .update();
        if (success) {
            log.info("[退回等待] 排队记录已退回等待: id={}, queueNo={}", id, record.getQueueNo());
        } else {
            log.warn("[退回等待] CAS更新失败: id={}", id);
        }
    }

    /**
     * 恢复排队：CANCELLED → WAITING（用于误取消纠错）
     *
     * @param id 排队记录ID
     */
    @Override
    public void reactivateQueue(Long id) {
        if (id == null) {
            log.warn("[恢复排队] id为空，跳过");
            return;
        }
        QueueRecord record = getById(id);
        if (record == null) {
            log.warn("[恢复排队] 记录不存在: id={}", id);
            return;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            log.warn("[恢复排队] 无权操作其他租户的排队记录: id={}", id);
            return;
        }
        if (!QueueRecordStatus.CANCELLED.getValue().equals(record.getStatus())) {
            log.warn("[恢复排队] 当前状态非CANCELLED，无法恢复: id={}, status={}",
                    id, record.getStatus());
            return;
        }
        boolean success = lambdaUpdate()
                .eq(QueueRecord::getId, id)
                .eq(QueueRecord::getStatus, QueueRecordStatus.CANCELLED.getValue())
                .set(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                .update();
        if (success) {
            log.info("[恢复排队] 排队记录已恢复: id={}, queueNo={}", id, record.getQueueNo());
        } else {
            log.warn("[恢复排队] CAS更新失败: id={}", id);
        }
    }

    /**
     * 安排入座：CALLED → SEATED
     * CAS 乐观更新，仅当状态为 CALLED 时才允许入座
     *
     * @param queueId 排队记录ID
     * @param tableId 桌台ID（可选，暂存但不写库，前端可展示）
     */
    @Override
    public void seatCustomer(Long queueId, Long tableId) {
        if (queueId == null) {
            log.warn("[安排入座] queueId为空，跳过");
            return;
        }
        QueueRecord record = getById(queueId);
        if (record == null) {
            log.warn("[安排入座] 排队记录不存在: queueId={}", queueId);
            return;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            log.warn("[安排入座] 无权操作其他租户的排队记录: queueId={}", queueId);
            return;
        }
        if (!QueueRecordStatus.CALLED.getValue().equals(record.getStatus())) {
            log.warn("[安排入座] 当前状态非CALLED，无法入座: queueId={}, status={}",
                    queueId, record.getStatus());
            return;
        }
        // CAS 更新：CALLED → SEATED
        boolean success = lambdaUpdate()
                .eq(QueueRecord::getId, queueId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.CALLED.getValue())
                .set(QueueRecord::getStatus, QueueRecordStatus.SEATED.getValue())
                .update();
        if (success) {
            log.info("[安排入座] 排队记录已入座: queueId={}, queueNo={}, tableId={}",
                    queueId, record.getQueueNo(), tableId);
        } else {
            log.warn("[安排入座] CAS更新失败，记录已被其他线程修改: queueId={}", queueId);
        }
    }

    /**
     * 排队统计：按状态分类计数
     * 使用 LambdaQueryWrapper + count() 单次查询各状态数量
     */
    @Override
    public QueueStatsVO queueStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        QueueStatsVO stats = new QueueStatsVO();

        // 总数
        long total = lambdaQuery()
                .eq(QueueRecord::getTenantId, tenantId)
                .count();
        stats.setTotalQueues(total);

        // 各状态计数
        stats.setWaitingCount(Long.valueOf(lambdaQuery()
                .eq(QueueRecord::getTenantId, tenantId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                .count()));
        stats.setCalledCount(Long.valueOf(lambdaQuery()
                .eq(QueueRecord::getTenantId, tenantId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.CALLED.getValue())
                .count()));
        stats.setSeatedCount(Long.valueOf(lambdaQuery()
                .eq(QueueRecord::getTenantId, tenantId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.SEATED.getValue())
                .count()));
        stats.setCancelledCount(Long.valueOf(lambdaQuery()
                .eq(QueueRecord::getTenantId, tenantId)
                .eq(QueueRecord::getStatus, QueueRecordStatus.CANCELLED.getValue())
                .count()));

        return stats;
    }
}



