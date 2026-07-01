package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dining/reservation")
@Tag(name = "预订管理")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<Reservation>> page(int page, int pageSize) {
        Page<Reservation> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Reservation> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Reservation::getReservedTime);
        reservationService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增预订")
    public R<Reservation> create(@RequestBody Map<String, Object> params) {
        String customerName = (String) params.get("customerName");
        String phone = (String) params.get("phone");
        String reservedTimeStr = (String) params.get("reservedTime");
        LocalDateTime reservedTime = LocalDateTime.parse(reservedTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Integer seatCount = params.get("seatCount") != null ? Integer.valueOf(params.get("seatCount").toString()) : null;
        Long tableId = params.get("tableId") != null ? Long.valueOf(params.get("tableId").toString()) : null;
        String remark = (String) params.get("remark");
        log.info("新增预订: customerName={}, phone={}", customerName, phone);
        Reservation r = reservationService.createReservation(customerName, phone, reservedTime, seatCount, tableId, remark);
        return R.success(r);
    }

    @PutMapping("/confirm/{id}")
    @Operation(summary = "确认预订")
    public R<String> confirm(@PathVariable Long id) {
        log.info("确认预订: {}", id);
        reservationService.confirmReservation(id);
        return R.success("确认预订成功");
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消预订")
    public R<String> cancel(@PathVariable Long id) {
        log.info("取消预订: {}", id);
        reservationService.cancelReservation(id);
        return R.success("取消预订成功");
    }

    @PutMapping("/arrive/{id}")
    @Operation(summary = "到店")
    public R<String> arrive(@PathVariable Long id) {
        log.info("到店: {}", id);
        reservationService.arrive(id);
        return R.success("到店成功");
    }
}
