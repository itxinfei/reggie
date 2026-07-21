package com.reggie.module.member.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.PointsRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 积分记录控制器
 * 提供会员积分记录的分页查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/member/points")
@Tag(name = "积分记录")
public class PointsRecordController {

    @Autowired
    private PointsRecordService pointsRecordService;

    @Autowired
    private MemberService memberService;

    /**
     * 分页查询会员积分记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param phone 会员手机号（可选，精确查询）
     * @return 积分记录分页结果（含关联会员信息）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员积分记录，支持按手机号搜索，返回关联会员信息（名称、手机号、余额）")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "phone", description = "会员手机号（可选，精确查询）")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, String phone,
                                       @RequestParam(required = false) String type) {
        Page<PointsRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<PointsRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(type != null && !type.isEmpty(), PointsRecord::getType, type);

        if (phone != null && !phone.isEmpty()) {
            LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
            memberQw.eq(Member::getPhone, phone);
            Member member = memberService.getOne(memberQw);
            if (member != null) {
                qw.eq(PointsRecord::getMemberId, member.getId());
            } else {
                // 手机号无匹配会员，返回空结果
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("records", Collections.emptyList());
                emptyResult.put("total", 0L);
                emptyResult.put("size", pageSize);
                emptyResult.put("current", (long) page);
                return R.success(emptyResult);
            }
        }

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(PointsRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(PointsRecord::getCreatedTime);
        pointsRecordService.page(pageInfo, qw);

        List<PointsRecord> records = pageInfo.getRecords();
        if (records.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("records", Collections.emptyList());
            result.put("total", pageInfo.getTotal());
            result.put("size", pageInfo.getSize());
            result.put("current", pageInfo.getCurrent());
            return R.success(result);
        }

        // 批量查询所有涉及的Member
        Set<Long> memberIds = records.stream()
                .map(PointsRecord::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
            memberQw.in(Member::getId, memberIds);
            memberQw.select(Member::getId, Member::getName, Member::getPhone, Member::getBalance);
            List<Member> members = memberService.list(memberQw);
            memberMap = members.stream().collect(Collectors.toMap(Member::getId, m -> m, (a, b) -> a));
        }

        // 组装增强后的记录列表
        List<Map<String, Object>> enhancedRecords = new ArrayList<>();
        for (PointsRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            Member m = memberMap.get(r.getMemberId());
            item.put("memberName", m != null ? m.getName() : "");
            item.put("phone", m != null ? m.getPhone() : "");
            item.put("type", r.getType());
            item.put("points", r.getPoints());
            item.put("balance", m != null ? m.getBalance() : null);
            item.put("createdTime", r.getCreatedTime());
            enhancedRecords.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", enhancedRecords);
        result.put("total", pageInfo.getTotal());
        result.put("size", pageInfo.getSize());
        result.put("current", pageInfo.getCurrent());
        return R.success(result);
    }

    /**
     * 积分统计
     * 返回全平台积分总览数据，包括总积分、今日/本月获取与消耗、近30天趋势
     */
    /**
     * 积分统计
     * @return 全平台积分总览数据（总积分、今日/本月获取与消耗、近30天趋势）
     */
    @GetMapping("/stats")
    @Operation(summary = "积分统计", description = "获取全平台积分统计数据：总积分、今日/本月获取与消耗、近30天趋势")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();

        // 1. 全平台累计积分（所有会员积分之和）
        LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
        if (tenantId != null) memberQw.eq(Member::getTenantId, tenantId);
        List<Member> allMembers = memberService.list(memberQw);
        long totalPoints = allMembers.stream().mapToLong(m -> m.getPoints() != null ? m.getPoints() : 0).sum();

        // 2. 今日获取/消耗积分
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<PointsRecord> todayQw = new LambdaQueryWrapper<>();
        if (tenantId != null) todayQw.eq(PointsRecord::getTenantId, tenantId);
        todayQw.between(PointsRecord::getCreatedTime, todayStart, todayEnd);
        List<PointsRecord> todayRecords = pointsRecordService.list(todayQw);
        long todayAcquired = todayRecords.stream()
                .filter(r -> "earn".equalsIgnoreCase(r.getType()) || "ACQUIRE".equalsIgnoreCase(r.getType()) || "IN".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
        long todayConsumed = todayRecords.stream()
                .filter(r -> "consume".equalsIgnoreCase(r.getType()) || "CONSUME".equalsIgnoreCase(r.getType()) || "OUT".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();

        // 3. 本月获取/消耗积分
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LocalDateTime monthEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<PointsRecord> monthQw = new LambdaQueryWrapper<>();
        if (tenantId != null) monthQw.eq(PointsRecord::getTenantId, tenantId);
        monthQw.between(PointsRecord::getCreatedTime, monthStart, monthEnd);
        List<PointsRecord> monthRecords = pointsRecordService.list(monthQw);
        long monthAcquired = monthRecords.stream()
                .filter(r -> "earn".equalsIgnoreCase(r.getType()) || "ACQUIRE".equalsIgnoreCase(r.getType()) || "IN".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
        long monthConsumed = monthRecords.stream()
                .filter(r -> "consume".equalsIgnoreCase(r.getType()) || "CONSUME".equalsIgnoreCase(r.getType()) || "OUT".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();

        // 4. 累计获取/消耗积分（全历史）
        LambdaQueryWrapper<PointsRecord> allQw = new LambdaQueryWrapper<>();
        if (tenantId != null) allQw.eq(PointsRecord::getTenantId, tenantId);
        List<PointsRecord> allRecords = pointsRecordService.list(allQw);
        long totalAcquired = allRecords.stream()
                .filter(r -> "earn".equalsIgnoreCase(r.getType()) || "ACQUIRE".equalsIgnoreCase(r.getType()) || "IN".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
        long totalConsumed = allRecords.stream()
                .filter(r -> "consume".equalsIgnoreCase(r.getType()) || "CONSUME".equalsIgnoreCase(r.getType()) || "OUT".equalsIgnoreCase(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();

        // 5. 近30天每日积分趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            long dayAcquired = allRecords.stream()
                    .filter(r -> {
                        if (r.getCreatedTime() == null) return false;
                        boolean inRange = !r.getCreatedTime().isBefore(ds) && !r.getCreatedTime().isAfter(de);
                        boolean isAcquire = "earn".equalsIgnoreCase(r.getType()) || "ACQUIRE".equalsIgnoreCase(r.getType()) || "IN".equalsIgnoreCase(r.getType());
                        return inRange && isAcquire;
                    })
                    .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
            long dayConsumed = allRecords.stream()
                    .filter(r -> {
                        if (r.getCreatedTime() == null) return false;
                        boolean inRange = !r.getCreatedTime().isBefore(ds) && !r.getCreatedTime().isAfter(de);
                        boolean isConsume = "consume".equalsIgnoreCase(r.getType()) || "CONSUME".equalsIgnoreCase(r.getType()) || "OUT".equalsIgnoreCase(r.getType());
                        return inRange && isConsume;
                    })
                    .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", date.toString());
            dayMap.put("acquired", dayAcquired);
            dayMap.put("consumed", dayConsumed);
            trend.add(dayMap);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPoints", totalPoints);
        result.put("todayAcquired", todayAcquired);
        result.put("todayConsumed", todayConsumed);
        result.put("monthAcquired", monthAcquired);
        result.put("monthConsumed", monthConsumed);
        result.put("totalAcquired", totalAcquired);
        result.put("totalConsumed", totalConsumed);
        result.put("trend", trend);
        return R.success(result);
    }
}
