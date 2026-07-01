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

@Service
public class QueueServiceImpl extends ServiceImpl<QueueMapper, QueueRecord> implements QueueService {

    @Override
    public QueueRecord takeNumber(Integer seatCount, String phone) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
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
