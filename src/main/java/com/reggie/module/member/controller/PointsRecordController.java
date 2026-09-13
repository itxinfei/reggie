package com.reggie.module.member.controller;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.PointsRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import com.reggie.dto.AdjustPointsDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分记录控制器
 * 提供会员积分记录的分页查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
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
    @RequireEmployee
    @Operation(summary = "分页查询", description = "分页查询会员积分记录，支持按手机号搜索，返回关联会员信息（名称、手机号、余额）")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "phone", description = "会员手机号（可选，精确查询）")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue =
            "10") @Min(1) @Max(100) int pageSize, String phone,
                                       @Parameter(description = "积分类型（earn-获取/consume-消耗，可选）") @RequestParam(required =
                                               false) String type) {
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
     * 积分调整
     * <p>运营手动调整会员积分：正数发放、负数扣减，并写入积分流水</p>
     *
     * @param dto 调整参数（会员ID + 积分变动数 + 说明）
     * @return 操作结果
     */
    @PostMapping("/adjust")
    @RequireEmployee
    @Operation(summary = "积分调整", description = "运营手动调整会员积分：正数发放、负数扣减，并写入积分流水")
    public R<String> adjust(@Parameter(description = "积分调整参数（会员ID、积分变动数、说明）", required =
            true) @Validated @RequestBody AdjustPointsDTO dto) {
        if (dto.getPoints() == null || dto.getPoints() == 0) {
            return R.error("积分变动数不能为0");
        }
        if (dto.getPoints() > 0) {
            memberService.addPoints(dto.getMemberId(), dto.getPoints(), "ADMIN_ADJUST", null);
        } else {
            memberService.deductPoints(dto.getMemberId(), -dto.getPoints(), "ADMIN_ADJUST", null);
        }
        // 补记调整说明到最新一条 ADMIN_ADJUST 流水（同一事务内，最新一条即刚写入的记录）
        if (dto.getRemark() != null && !dto.getRemark().trim().isEmpty()) {
            PointsRecord latest = pointsRecordService.lambdaQuery()
                    .eq(PointsRecord::getMemberId, dto.getMemberId())
                    .eq(PointsRecord::getBizType, "ADMIN_ADJUST")
                    .orderByDesc(PointsRecord::getCreatedTime)
                    .last("limit 1")
                    .one();
            if (latest != null) {
                latest.setRemark(dto.getRemark().trim());
                pointsRecordService.updateById(latest);
            }
        }
        return R.success("积分调整成功");
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
    @RequireEmployee
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
        long todayAcquired = sumAcquired(todayRecords);
        long todayConsumed = sumConsumed(todayRecords);

        // 3. 本月获取/消耗积分
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LocalDateTime monthEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<PointsRecord> monthQw = new LambdaQueryWrapper<>();
        if (tenantId != null) monthQw.eq(PointsRecord::getTenantId, tenantId);
        monthQw.between(PointsRecord::getCreatedTime, monthStart, monthEnd);
        List<PointsRecord> monthRecords = pointsRecordService.list(monthQw);
        long monthAcquired = sumAcquired(monthRecords);
        long monthConsumed = sumConsumed(monthRecords);

        // 4. 累计获取/消耗积分（全历史）
        LambdaQueryWrapper<PointsRecord> allQw = new LambdaQueryWrapper<>();
        if (tenantId != null) allQw.eq(PointsRecord::getTenantId, tenantId);
        List<PointsRecord> allRecords = pointsRecordService.list(allQw);
        long totalAcquired = sumAcquired(allRecords);
        long totalConsumed = sumConsumed(allRecords);

        // 5. 近30天每日积分趋势（等价抽取）
        List<Map<String, Object>> trend = buildDailyTrend(allRecords);

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

    /**
     * 是否为获取积分类型（等价抽取）。
     */
    private boolean isAcquire(String type) {
        return "earn".equalsIgnoreCase(type) || "ACQUIRE".equalsIgnoreCase(type) || "IN".equalsIgnoreCase(type);
    }

    /**
     * 是否为消耗积分类型（等价抽取）。
     */
    private boolean isConsume(String type) {
        return "consume".equalsIgnoreCase(type) || "CONSUME".equalsIgnoreCase(type) || "OUT".equalsIgnoreCase(type);
    }

    /**
     * 汇总获取积分（等价抽取）。
     */
    private long sumAcquired(List<PointsRecord> records) {
        return records.stream()
                .filter(r -> isAcquire(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
    }

    /**
     * 汇总消耗积分（等价抽取）。
     */
    private long sumConsumed(List<PointsRecord> records) {
        return records.stream()
                .filter(r -> isConsume(r.getType()))
                .mapToLong(r -> r.getPoints() != null ? r.getPoints() : 0).sum();
    }

    /**
     * 构建近30天每日积分趋势（等价抽取，降低方法长度）。
     */
    private List<Map<String, Object>> buildDailyTrend(List<PointsRecord> allRecords) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            List<PointsRecord> dayRecords = allRecords.stream()
                    .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(ds)
                            && !r.getCreatedTime().isAfter(de))
                    .collect(Collectors.toList());
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", date.toString());
            dayMap.put("acquired", sumAcquired(dayRecords));
            dayMap.put("consumed", sumConsumed(dayRecords));
            trend.add(dayMap);
        }
        return trend;
    }
}
