package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.mapper.ReservationMapper;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    @Autowired
    private DiningTableService diningTableService;

    @Override
    public Reservation createReservation(String customerName, String phone, LocalDateTime reservedTime, Integer seatCount, Long tableId, String remark) {
        Reservation r = new Reservation();
        r.setTenantId(BaseContext.getCurrentTenantId());
        r.setCustomerName(customerName);
        r.setPhone(phone);
        r.setReservedTime(reservedTime);
        r.setSeatCount(seatCount);
        r.setTableId(tableId);
        r.setRemark(remark);
        r.setStatus("PENDING");
        save(r);
        return r;
    }

    @Override
    public void confirmReservation(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            r.setStatus("CONFIRMED");
            updateById(r);
        }
    }

    @Override
    public void cancelReservation(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            r.setStatus("CANCELLED");
            updateById(r);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            r.setStatus("ARRIVED");
            updateById(r);
            if (r.getTableId() != null) {
                diningTableService.changeStatus(r.getTableId(), "OCCUPIED");
            }
        }
    }
}
