package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.vo.QueueStatsVO;

/**
 * <p>
 * 排队取号服务接口
 * </p>
 * <p>提供顾客取号、叫号、取消排队等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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
     * C 端顾客自助取号（携带用户ID，便于查询/取消归属校验）
     *
     * @param seatCount 就座人数
     * @param phone     顾客手机号
     * @param userId    顾客用户ID
     * @return 排队记录
     */
    QueueRecord takeNumber(Integer seatCount, String phone, Long userId);

    /**
     * 顾客取消自己的排队（WAITING/CALLED 均可），非本人记录抛业务异常
     *
     * @param id     排队记录ID
     * @param userId 顾客用户ID
     */
    void cancelMyQueue(Long id, Long userId);

    /**
     * 前面还在等待的桌数（同租户 WAITING 且取号早于本条），供顾客端展示进度
     *
     * @param queueId 排队记录ID
     * @return 等待桌数；记录不存在返回 0
     */
    long countWaitingAhead(Long queueId);

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

    /**
     * 安排入座：CALLED → SEATED
     *
     * @param queueId 排队记录ID
     * @param tableId 桌台ID（可选）
     */
    void seatCustomer(Long queueId, Long tableId);

    /**
     * 退回等待：CALLED → WAITING
     *
     * @param id 排队记录ID
     */
    void recallQueue(Long id);

    /**
     * 恢复排队：CANCELLED → WAITING
     *
     * @param id 排队记录ID
     */
    void reactivateQueue(Long id);

    /**
     * 排队统计（按状态分类计数）
     *
     * @return 排队统计
     */
    QueueStatsVO queueStats();
}
