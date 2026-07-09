package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.CreateReservationDTO;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/dining/reservation")
@Tag(name = "预订管理")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询预订记录列表，支持按状态、姓名、手机号、日期筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "status", description = "状态（可选）：PENDING-待确认, CONFIRMED-已确认, ARRIVED-已到店, CANCELLED-已取消")
    @Parameter(name = "customerName", description = "客户姓名（可选，模糊搜索）")
    @Parameter(name = "phone", description = "手机号（可选，模糊搜索）")
    @Parameter(name = "reservedDate", description = "预订日期（可选，格式yyyy-MM-dd）")
    public R<Page<Reservation>> page(int page, int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String customerName,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String reservedDate) {
        Page<Reservation> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Reservation> qw = new LambdaQueryWrapper<>();
        // 修改点：新增状态、姓名、手机号、日期筛选条件
        qw.eq(status != null && !status.isEmpty(), Reservation::getStatus, status);
        qw.like(customerName != null && !customerName.isEmpty(), Reservation::getCustomerName, customerName);
        qw.like(phone != null && !phone.isEmpty(), Reservation::getPhone, phone);
        if (reservedDate != null && !reservedDate.isEmpty()) {
            qw.apply("DATE(reserved_time) = {0}", reservedDate);
        }
        qw.orderByDesc(Reservation::getReservedTime);
        reservationService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增预订", description = "创建新的预订记录，支持指定桌台和人数")
    public R<Reservation> create(@Valid @RequestBody CreateReservationDTO dto) {
        log.info("新增预订: customerName={}, phone={}", dto.getCustomerName(),
            dto.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        Reservation r = reservationService.createReservation(
            dto.getCustomerName(), dto.getPhone(), dto.getReservedTime(),
            dto.getSeatCount(), dto.getTableId(), dto.getRemark());
        return R.success(r);
    }

    @PutMapping("/confirm/{id}")
    @Operation(summary = "确认预订", description = "确认预订信息，标记为已确认状态")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> confirm(@PathVariable Long id) {
        log.info("确认预订: {}", id);
        reservationService.confirmReservation(id);
        return R.success("确认预订成功");
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消预订", description = "取消指定预订记录")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> cancel(@PathVariable Long id) {
        log.info("取消预订: {}", id);
        reservationService.cancelReservation(id);
        return R.success("取消预订成功");
    }

    @PutMapping("/arrive/{id}")
    @Operation(summary = "到店", description = "标记顾客已到店")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> arrive(@PathVariable Long id) {
        log.info("到店: {}", id);
        reservationService.arrive(id);
        return R.success("到店成功");
    }
}

