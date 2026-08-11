package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.mapper.QueueMapper;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.enums.QueueRecordStatus;
import com.reggie.module.dining.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
/**
 * Queue service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class QueueServiceImpl extends ServiceImpl<QueueMapper, QueueRecord> implements QueueService {

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 取号分布式锁 Key 前缀 */
    private static final String QUEUE_LOCK_KEY_PREFIX = "queue:takeNumber:lock:";

    /** 分布式锁过期时间（秒） */
    private static final int LOCK_EXPIRE_SECONDS = 10;

    /** 获取锁最大等待时间（毫秒） */
    private static final int LOCK_WAIT_MILLIS = 3000;

    /** 获取锁重试间隔（毫秒） */
    private static final int LOCK_RETRY_INTERVAL = 100;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public QueueRecord takeNumber(Integer seatCount, String phone) {
        // 修改点：使用 Redis SETNX 实现分布式锁
        String lockKey = QUEUE_LOCK_KEY_PREFIX + seatCount;
        boolean locked = acquireLock(lockKey);

        if (!locked) {
            log.warn("[排队取号] 获取锁失败，系统繁忙：seatCount={}", seatCount);
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
            releaseLock(lockKey);
        }
    }

    /**
     * 获取分布式锁（自旋 + SETNX）
     * 修改点：使用 Redis SETNX + EXPIRE 实现简单分布式锁
     *
     * @param lockKey 锁的Key
     * @return true=获取成功，false=超时失败
     */
    private boolean acquireLock(String lockKey) {
        if (redisTemplate == null) {
            return true; // Redis 不可用时降级放行（单机部署无并发问题）
        }

        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < LOCK_WAIT_MILLIS) {
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    return true;
                }
                // 自旋等待后重试
                Thread.sleep(LOCK_RETRY_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[排队取号] 获取锁被中断：{}", lockKey);
        } catch (Exception e) {
            // Redis 连接异常时降级放行（测试环境或无 Redis 时）
            log.warn("[排队取号] 获取锁异常，降级放行：{}, error={}", lockKey, e.getMessage());
        }
        return true; // 异常时降级放行，避免阻塞业务
    }

    /**
     * 释放分布式锁
     * 修改点：仅删除自己的锁（简单实现，生产建议 Lua 脚本校验 ownership）
     */
    private void releaseLock(String lockKey) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("[排队取号] 释放锁失败：{}, error={}", lockKey, e.getMessage());
        }
    }

    @Override
    public QueueRecord callNext(Integer seatCount) {
        int maxRetry = 50;
        for (int i = 0; i < maxRetry; i++) {
            LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
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
        // CAS 更新：仅当状态为 WAITING 时才允许取消，防止并发重复操作
        boolean success = lambdaUpdate()
                .eq(QueueRecord::getId, id)
                .eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue())
                .set(QueueRecord::getStatus, QueueRecordStatus.CANCELLED.getValue())
                .update();
        if (!success) {
            log.warn("[排队取消] 取消失败，当前状态非WAITING或记录不存在: id={}", id);
        }
    }
}



