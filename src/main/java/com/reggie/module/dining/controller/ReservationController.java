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
import com.reggie.common.LogMaskUtils;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.mapper.ReservationMapper;
import com.reggie.module.dining.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private ReservationMapper reservationMapper;

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
    public R<Page<Reservation>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue =
            "10") @Min(1) @Max(100) int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String customerName,
                                     @Parameter(description = "手机号（可选，模糊搜索）")
                                     @RequestParam(required = false) String phone,
                                     @Parameter(description = "预订日期（可选，格式yyyy-MM-dd）")
                                     @RequestParam(required = false) String reservedDate,
                                     @Parameter(description = "开始日期（可选，格式yyyy-MM-dd）")
                                     @RequestParam(required = false) String beginTime,
                                     @Parameter(description = "结束日期（可选，格式yyyy-MM-dd）")
                                     @RequestParam(required = false) String endTime) {
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
        // 日期筛选：优先 beginTime/endTime 范围查询，其次 reservedDate 单日查询
        if (beginTime != null && !beginTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            qw.ge(Reservation::getReservedTime, beginTime + " 00:00:00");
            qw.le(Reservation::getReservedTime, endTime + " 23:59:59");
        } else if (reservedDate != null && !reservedDate.isEmpty()) {
            qw.apply("DATE(reserved_time) = {0}", reservedDate);
        }
        qw.orderByDesc(Reservation::getReservedTime);
        reservationService.page(pageInfo, qw);

        // 回填桌台名称
        List<Reservation> records = pageInfo.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> tableIds = records.stream()
                    .map(Reservation::getTableId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!tableIds.isEmpty()) {
                List<DiningTable> tables = diningTableService.listByIds(tableIds);
                Map<Long, String> tableNameMap = new HashMap<>();
                tables.forEach(t -> {
                    if (t == null || t.getId() == null) {
                        return;
                    }
                    tableNameMap.put(t.getId(), t.getName() != null ? t.getName() : "");
                });
                for (Reservation r : records) {
                    r.setTableName(tableNameMap.get(r.getTableId()));
                }
            }
        }
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
    public R<Reservation> create(@Parameter(description = "预订请求（客户姓名、手机号、预订时间、人数、桌台ID、备注）", required =
            true) @Valid @RequestBody CreateReservationDTO dto) {
        log.info("新增预订: customerName={}, phone={}", dto.getCustomerName(),
            LogMaskUtils.maskPhone(dto.getPhone()));
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

    /**
     * 查询单条预订详情
     * @param id 预订ID
     * @return 预订详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "预订详情", description = "查询单条预订记录详情")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<Reservation> getById(@PathVariable Long id) {
        Reservation r = reservationService.getById(id);
        if (r == null) {
            return R.error("预订不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(r.getTenantId())) {
            return R.error("无权查看其他租户的预订");
        }
        // 回填桌台名称
        if (r.getTableId() != null) {
            DiningTable table = diningTableService.getById(r.getTableId());
            if (table != null) {
                r.setTableName(table.getName());
            }
        }
        return R.success(r);
    }

    /**
     * 编辑预订（仅待确认/已确认状态允许修改）
     * @param dto 预订请求
     * @return 更新后的预订记录
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "编辑预订", description = "修改预订信息，仅待确认/已确认状态允许修改")
    public R<Reservation> update(@Valid @RequestBody CreateReservationDTO dto) {
        if (dto.getId() == null) {
            return R.error("预订ID不能为空");
        }
        log.info("编辑预订: id={}, customerName={}, phone={}", dto.getId(), dto.getCustomerName(),
            LogMaskUtils.maskPhone(dto.getPhone()));
        try {
            Reservation r = reservationService.updateReservation(
                    dto.getId(), dto.getCustomerName(), dto.getPhone(),
                    dto.getReservedTime(), dto.getSeatCount(), dto.getTableId(), dto.getRemark());
            return R.success(r);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除预订（仅已取消状态允许删除）
     * @param id 预订ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除预订", description = "删除已取消的预订记录")
    @Parameter(name = "id", description = "预订ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除预订: {}", id);
        try {
            reservationService.deleteReservation(id);
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
        return R.success("删除成功");
    }

    /**
     * 预订统计（按状态聚合计数，替代前端 5 次 pageSize:1 调用）
     */
    @GetMapping("/stats")
    @Operation(summary = "预订统计", description = "按状态统计预订数量")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        // 使用 SQL 聚合查询，避免全量加载到内存
        List<Map<String, Object>> rows = reservationMapper.countByStatus(tenantId);
        long total = 0, pending = 0, confirmed = 0, arrived = 0, cancelled = 0;
        for (Map<String, Object> row : rows) {
            String s = String.valueOf(row.get("status"));
            Object cntVal = row.get("cnt");
            long cnt = (cntVal instanceof Number) ? ((Number) cntVal).longValue() : 0;
            total += cnt;
            if ("PENDING".equals(s)) { pending = cnt; }
            else if ("CONFIRMED".equals(s)) { confirmed = cnt; }
            else if ("ARRIVED".equals(s)) { arrived = cnt; }
            else if ("CANCELLED".equals(s)) { cancelled = cnt; }
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReservations", total);
        stats.put("pendingCount", pending);
        stats.put("confirmedCount", confirmed);
        stats.put("arrivedCount", arrived);
        stats.put("cancelledCount", cancelled);
        return R.success(stats);
    }
}


