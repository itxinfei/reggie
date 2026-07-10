package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.mapper.QueueMapper;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.enums.QueueRecordStatus;
import com.reggie.module.dining.service.QueueService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 排队服务实现
 *
 * 并发安全说明：
 * takeNumber() 方法存在并发安全问题，在高并发场景下可能生成重复的排队号。
 * 建议使用分布式锁（如 Redisson）进行优化：
 *
 * 优化方案（需要引入 Redisson 依赖）：
 * <pre>
 * private static final String QUEUE_LOCK_KEY = "queue:takeNumber:lock:";
 *
 * &#64;Override
 * public QueueRecord takeNumber(Integer seatCount, String phone) {
 *     String lockKey = QUEUE_LOCK_KEY + seatCount;
 *     RLock lock = redissonClient.getLock(lockKey);
 *     try {
 *         // 尝试获取锁，最多等待5秒，锁10秒后自动释放
 *         if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
 *             throw new RuntimeException("系统繁忙，请稍后重试");
 *         }
 *         // ... 原有业务逻辑
 *     } finally {
 *         if (lock.isHeldByCurrentThread()) {
 *             lock.unlock();
 *         }
 *     }
 * }
 * </pre>
 *
 * 临时解决方案：在应用层通过 nginx 或网关限流，或者将取号请求放入消息队列串行化处理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class QueueServiceImpl extends ServiceImpl<QueueMapper, QueueRecord> implements QueueService {

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public QueueRecord takeNumber(Integer seatCount, String phone) {
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
    }

    @Override
    public QueueRecord callNext(Integer seatCount) {
        LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(QueueRecord::getStatus, QueueRecordStatus.WAITING.getValue());
        if (seatCount != null) qw.eq(QueueRecord::getSeatCount, seatCount);
        qw.orderByAsc(QueueRecord::getCreatedTime);
        qw.last("LIMIT 1");
        QueueRecord record = getOne(qw);
        if (record != null) {
            record.setStatus(QueueRecordStatus.CALLED.getValue());
            updateById(record);
        }
        return record;
    }

    @Override
    public void cancelQueue(Long id) {
        QueueRecord record = getById(id);
        if (record != null) {
            record.setStatus(QueueRecordStatus.CANCELLED.getValue());
            updateById(record);
        }
    }
}
