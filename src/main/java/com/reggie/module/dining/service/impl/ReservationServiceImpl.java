package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
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
import java.util.Map;
import java.util.Objects;

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

    /**
     * 创建 reservation。
     * @param customerName 参数 customerName
     * @param phone 参数 phone
     * @param reservedTime 参数 reservedTime
     * @param seatCount 参数 seatCount
     * @param tableId 参数 tableId
     * @param remark 参数 remark
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation createReservation(String customerName, String phone, LocalDateTime reservedTime,
            Integer seatCount, Long tableId, String remark) {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        // 修复 P2-3：时间冲突检测——同一桌台同一时间窗口（±1小时）已被预订则拒绝
        if (tableId != null && reservedTime != null) {
            LambdaQueryWrapper<Reservation> conflictQw = new LambdaQueryWrapper<>();
            conflictQw.eq(Reservation::getTableId, tableId)
                    .eq(Reservation::getTenantId, currentTenantId)
                    .in(Reservation::getStatus,
                            ReservationStatus.PENDING.getValue(),
                            ReservationStatus.CONFIRMED.getValue())
                    .ge(Reservation::getReservedTime, reservedTime.minusHours(1))
                    .le(Reservation::getReservedTime, reservedTime.plusHours(1));
            long conflictCount = count(conflictQw);
            if (conflictCount > 0) {
                throw new CustomException("该桌台在相近时段已被预订，请选择其他桌台或时间");
            }
        }
        Reservation r = new Reservation();
        r.setTenantId(currentTenantId);
        r.setCustomerName(customerName);
        r.setPhone(phone);
        r.setReservedTime(reservedTime);
        r.setSeatCount(seatCount);
        r.setTableId(tableId);
        r.setRemark(remark);
        r.setUpdateTime(LocalDateTime.now());
        r.setStatus(ReservationStatus.PENDING.getValue());
        save(r);
        return r;
    }

    /**
     * 确认 reservation。
     * @param id 参数 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReservation(Long id) {
        Reservation r = getById(id);
        if (r == null) {
            throw new CustomException("预订不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(r.getTenantId())) {
            throw new CustomException("无权操作其他租户的预订");
        }
        if (r.getTableId() != null) {
            diningTableService.changeStatus(r.getTableId(), DiningTableStatus.RESERVED.getValue());
        }
        r.setStatus(ReservationStatus.CONFIRMED.getValue());
        updateById(r);
    }

    /**
     * 取消 reservation。
     * @param id 参数 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long id) {
        Reservation r = getById(id);
        if (r == null) {
            throw new CustomException("预订不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(r.getTenantId())) {
            throw new CustomException("无权操作其他租户的预订");
        }
        // 释放桌台：仅 CONFIRMED 预订在确认时把桌台置为 RESERVED，取消须还原 FREE，
        // 否则桌台永久卡在预留态无法接客（PENDING 未占桌台，无需释放）
        boolean wasConfirmed = ReservationStatus.CONFIRMED.getValue().equals(r.getStatus());
        r.setStatus(ReservationStatus.CANCELLED.getValue());
        updateById(r);
        if (wasConfirmed && r.getTableId() != null) {
            diningTableService.changeStatus(r.getTableId(), DiningTableStatus.FREE.getValue());
        }
    }

    /**
     * 处理 arrive。
     * @param id 参数 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long id) {
        Reservation r = getById(id);
        if (r == null) {
            throw new CustomException("预订不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(r.getTenantId())) {
            throw new CustomException("无权操作其他租户的预订");
        }
        r.setStatus(ReservationStatus.ARRIVED.getValue());
        updateById(r);
        if (r.getTableId() != null) {
            // 到店即开台：预订确认时桌台为 RESERVED，先在本事务内释放回 FREE，再一键开台
            // 建 EAT_IN 占位订单并置占用，修复旧实现裸改占用却不建单、结账无单可结的问题。
            diningTableService.changeStatus(r.getTableId(), DiningTableStatus.FREE.getValue());
            Map<String, Object> openResult = diningTableService.openWithOrder(
                    r.getTableId(), r.getSeatCount(), "预订到店 " + r.getCustomerName());
            log.info("[预订到店] 已开台: reservationId={}, tableId={}, orderId={}",
                    id, r.getTableId(), openResult.get("orderId"));
        }
    }

    /**
     * 更新预订信息（仅待确认/已确认状态允许修改）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation updateReservation(Long id, String customerName, String phone,
            LocalDateTime reservedTime, Integer seatCount, Long tableId, String remark) {
        Reservation r = getById(id);
        if (r == null) {
            throw new CustomException("预订不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(r.getTenantId())) {
            throw new CustomException("无权操作其他租户的预订");
        }
        // 仅待确认/已确认状态允许修改
        if (!ReservationStatus.PENDING.getValue().equals(r.getStatus())
                && !ReservationStatus.CONFIRMED.getValue().equals(r.getStatus())) {
            throw new CustomException("当前状态不允许修改预订信息");
        }
        // 桌台变更时检测冲突
        if (tableId != null && reservedTime != null
                && (!tableId.equals(r.getTableId()) || !reservedTime.equals(r.getReservedTime()))) {
            LambdaQueryWrapper<Reservation> conflictQw = new LambdaQueryWrapper<>();
            conflictQw.eq(Reservation::getTableId, tableId)
                    .eq(Reservation::getTenantId, currentTenantId)
                    .ne(Reservation::getId, id)
                    .in(Reservation::getStatus,
                            ReservationStatus.PENDING.getValue(),
                            ReservationStatus.CONFIRMED.getValue())
                    .ge(Reservation::getReservedTime, reservedTime.minusHours(1))
                    .le(Reservation::getReservedTime, reservedTime.plusHours(1));
            long conflictCount = count(conflictQw);
            if (conflictCount > 0) {
                throw new CustomException("该桌台在相近时段已被预订，请选择其他桌台或时间");
            }
        }
        // 桌台变更时处理原桌台状态还原
        if (r.getTableId() != null && !r.getTableId().equals(tableId)
                && ReservationStatus.CONFIRMED.getValue().equals(r.getStatus())) {
            diningTableService.changeStatus(r.getTableId(), DiningTableStatus.FREE.getValue());
        }
        r.setCustomerName(customerName);
        r.setPhone(phone);
        r.setReservedTime(reservedTime);
        r.setSeatCount(seatCount);
        r.setTableId(tableId);
        r.setRemark(remark);
        r.setUpdateTime(LocalDateTime.now());
        updateById(r);
        // 新桌台已确认状态设为已预订
        if (tableId != null && ReservationStatus.CONFIRMED.getValue().equals(r.getStatus())) {
            diningTableService.changeStatus(tableId, DiningTableStatus.RESERVED.getValue());
        }
        return r;
    }

    /**
     * 删除预订（仅已取消状态允许删除，防止误删有效预订）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReservation(Long id) {
        Reservation r = getById(id);
        if (r == null) {
            throw new CustomException("预订不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(r.getTenantId())) {
            throw new CustomException("无权操作其他租户的预订");
        }
        // 仅已取消状态允许删除
        if (!ReservationStatus.CANCELLED.getValue().equals(r.getStatus())) {
            throw new CustomException("只有已取消的预订才能删除，请先取消预订");
        }
        removeById(id);
    }
}
