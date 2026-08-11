package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.mapper.ReservationMapper;
import com.reggie.module.dining.model.Reservation;
import com.reggie.enums.ReservationStatus;
import com.reggie.enums.DiningTableStatus;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 预订服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    /** 堂食桌台服务 */
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
        r.setStatus(ReservationStatus.PENDING.getValue());
        save(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReservation(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            // 确认预订时锁定桌台：FREE → RESERVED
            if (r.getTableId() != null) {
                diningTableService.changeStatus(r.getTableId(), DiningTableStatus.RESERVED.getValue());
            }
            r.setStatus(ReservationStatus.CONFIRMED.getValue());
            updateById(r);
        }
    }

    @Override
    public void cancelReservation(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            r.setStatus(ReservationStatus.CANCELLED.getValue());
            updateById(r);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long id) {
        Reservation r = getById(id);
        if (r != null) {
            r.setStatus(ReservationStatus.ARRIVED.getValue());
            updateById(r);
            if (r.getTableId() != null) {
                try {
                    diningTableService.changeStatus(r.getTableId(), DiningTableStatus.OCCUPIED.getValue());
                } catch (Exception e) {
                    // 桌台状态变更失败不回滚预订状态，仅记录日志
                    log.warn("[预订到店] 桌台状态变更失败，不回滚预订状态: tableId={}, error={}", r.getTableId(), e.getMessage());
                }
            }
        }
    }
}
