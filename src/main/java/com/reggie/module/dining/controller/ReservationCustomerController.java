package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireUser;
import com.reggie.module.dining.dto.CustomerReservationDTO;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * C 端顾客到店预订控制器。
 * 与员工端 {@link ReservationController}（/api/dining/reservation，@RequireEmployee）分离：
 * 本控制器只暴露创建预订 / 查本人预订 / 本人取消。
 *
 * @author reggie
 * @since 2026-09-29
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/dining/reservation/customer")
@Tag(name = "顾客到店预订")
@RequireUser
public class ReservationCustomerController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    /**
     * 顾客创建预订（姓名/手机自动带入登录用户，姓名允许修改）
     */
    @PostMapping
    @Operation(summary = "创建到店预订")
    public R<Reservation> create(@Valid @RequestBody CustomerReservationDTO dto) {
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);
        String customerName = dto.getCustomerName();
        if ((customerName == null || customerName.trim().isEmpty()) && user != null) {
            customerName = user.getName();
        }
        String phone = user != null ? user.getPhone() : null;
        log.info("[顾客预订] userId={}, reservedTime={}, seatCount={}", userId, dto.getReservedTime(),
                dto.getSeatCount());
        Reservation reservation = reservationService.createReservation(customerName, phone,
                dto.getReservedTime(), dto.getSeatCount(), dto.getTableId(), dto.getRemark(), userId);
        return R.success(reservation);
    }

    /**
     * 我的预订列表（最新在前）
     */
    @GetMapping("/my")
    @Operation(summary = "我的预订列表")
    public R<List<Reservation>> my() {
        Long userId = BaseContext.getCurrentId();
        List<Reservation> list = reservationService.lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getReservedTime)
                .list();
        return R.success(list);
    }

    /**
     * 顾客取消自己的预订（CONFIRMED 会释放桌台）
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消我的预订")
    public R<String> cancel(@PathVariable Long id) {
        reservationService.cancelMyReservation(id, BaseContext.getCurrentId());
        return R.success("取消预订成功");
    }
}
