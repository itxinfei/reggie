package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.QueueRecord;

public interface QueueService extends IService<QueueRecord> {
    QueueRecord takeNumber(Integer seatCount, String phone);
    QueueRecord callNext(Integer seatCount);
    void cancelQueue(Long id);
}
