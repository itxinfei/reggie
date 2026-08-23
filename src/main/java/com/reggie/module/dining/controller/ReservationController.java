package com.reggie.module.dining.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.CreateReservationDTO;
import com.reggie.common.CustomException;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.format.DateTimeFormatter;

/**
 * 预订管理控制器
 * 提供桌台预订的创建、确认、取消、到店等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/dining/reservation")
@Tag(name = "预订管理")
@RequireEmployee
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询预订记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 预订状态（可选）
     * @param customerName 客户姓名（可选，模糊搜索）
     * @param phone 手机号（可选，模糊搜索）
     * @param reservedDate 预订日期（可选，格式yyyy-MM-dd）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询预订记录列表，支持按状态、姓名、手机号、日期筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "status", description = "状态（可选）：PENDING-待确认, CONFIRMED-已确认, ARRIVED-已到店, CANCELLED-已取消")
    @Parameter(name = "customerName", description = "客户姓名（可选，模糊搜索）")
    @Parameter(name = "phone", description = "手机号（可选，模糊搜索）")
    @Parameter(name = "reservedDate", description = "预订日期（可选，格式yyyy-MM-dd）")
    public R<Page<Reservation>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String customerName,
                                     @Parameter(description = "Phone")
                                     @RequestParam(required = false) String phone,
                                     @Parameter(description = "ReservedDate")
                                     @RequestParam(required = false) String reservedDate) {
        Page<Reservation> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Reservation> qw = new LambdaQueryWrapper<>();
        // 强制租户过滤，防止跨租户数据泄露
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        qw.eq(Reservation::getTenantId, tenantId);
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

    /**
     * 新增预订
     * @param dto 预订请求
     * @return 预订记录
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增预订", description = "创建新的预订记录，支持指定桌台和人数")
    public R<Reservation> create(@Valid @RequestBody CreateReservationDTO dto) {
        log.info("新增预订: customerName={}, phone={}", dto.getCustomerName(),
            dto.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        Reservation r = reservationService.createReservation(
            dto.getCustomerName(), dto.getPhone(), dto.getReservedTime(),
            dto.getSeatCount(), dto.getTableId(), dto.getRemark());
        return R.success(r);
    }

    /**
     * 确认预订
     * @param id 预订ID
     * @return 操作结果
     */
    @PutMapping("/confirm/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "确认预订", description = "确认预订信息，标记为已确认状态")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> confirm(@PathVariable Long id) {
        log.info("确认预订: {}", id);
        Reservation r = reservationService.getById(id);
        if (r == null) {
            log.warn("确认预订时预订不存在，幂等返回成功: id={}", id);
            return R.success("确认预订成功");
        }
        try {
            reservationService.confirmReservation(id);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
        return R.success("确认预订成功");
    }

    /**
     * 取消预订
     * @param id 预订ID
     * @return 操作结果
     */
    @PutMapping("/cancel/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "取消预订", description = "取消指定预订记录")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> cancel(@PathVariable Long id) {
        log.info("取消预订: {}", id);
        try {
            reservationService.cancelReservation(id);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
        return R.success("取消预订成功");
    }

    /**
     * 标记顾客已到店
     * @param id 预订ID
     * @return 操作结果
     */
    @PutMapping("/arrive/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "到店", description = "标记顾客已到店")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> arrive(@PathVariable Long id) {
        log.info("到店: {}", id);
        Reservation r = reservationService.getById(id);
        if (r == null) {
            log.warn("到店时预订不存在，幂等返回成功: id={}", id);
            return R.success("到店成功");
        }
        try {
            reservationService.arrive(id);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
        return R.success("到店成功");
    }
}


