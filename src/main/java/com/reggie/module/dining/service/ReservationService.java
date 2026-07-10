package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.Reservation;
import java.time.LocalDateTime;

/**
 * 预订服务接口
 * 提供桌台预订、确认、取消、到店签到等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface ReservationService extends IService<Reservation> {

    /**
     * 创建预订记录
     *
     * @param customerName 客户姓名
     * @param phone        客户手机号
     * @param reservedTime 预订时间
     * @param seatCount    就座人数
     * @param tableId      指定桌台ID（可为null）
     * @param remark       备注
     * @return 预订记录
     */
    Reservation createReservation(String customerName, String phone, LocalDateTime reservedTime, Integer seatCount, Long tableId, String remark);

    /**
     * 确认预订
     *
     * @param id 预订记录ID
     */
    void confirmReservation(Long id);

    /**
     * 取消预订
     *
     * @param id 预订记录ID
     */
    void cancelReservation(Long id);

    /**
     * 到店签到
     *
     * @param id 预订记录ID
     */
    void arrive(Long id);
}
