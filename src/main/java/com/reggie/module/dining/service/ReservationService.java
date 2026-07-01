package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.Reservation;
import java.time.LocalDateTime;

public interface ReservationService extends IService<Reservation> {
    Reservation createReservation(String customerName, String phone, LocalDateTime reservedTime, Integer seatCount, Long tableId, String remark);
    void confirmReservation(Long id);
    void cancelReservation(Long id);
    void arrive(Long id);
}
