package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.QueueRecord;

/**
 * 排队取号服务接口
 * 提供顾客取号、叫号、取消排队等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface QueueService extends IService<QueueRecord> {

    /**
     * 顾客取号排队
     *
     * @param seatCount 就座人数
     * @param phone     顾客手机号
     * @return 排队记录
     */
    QueueRecord takeNumber(Integer seatCount, String phone);

    /**
     * 叫号（按座位数匹配下一位排队顾客）
     *
     * @param seatCount 可提供的座位数
     * @return 被叫到的排队记录
     */
    QueueRecord callNext(Integer seatCount);

    /**
     * 取消排队
     *
     * @param id 排队记录ID
     */
    void cancelQueue(Long id);
}
