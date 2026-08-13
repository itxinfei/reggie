package com.reggie.module.member.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * 充值记录控制器
 * 提供会员充值记录的分页查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/api/member/recharge")
@Tag(name = "充值记录")
public class RechargeRecordController {

    @Autowired
    private RechargeRecordService rechargeRecordService;

    @Autowired
    private MemberService memberService;

    /**
     * 分页查询会员充值记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param phone 会员手机号（可选，精确查询）
     * @return 充值记录分页结果（含关联会员信息）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员充值记录，支持按手机号搜索，返回关联会员信息（名称、手机号）")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "phone", description = "会员手机号（可选，精确查询）")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, String phone,
                                       @RequestParam(required = false) String paymentMethod) {
        Page<RechargeRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<RechargeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(paymentMethod != null && !paymentMethod.isEmpty(), RechargeRecord::getPaymentMethod, paymentMethod);

        if (phone != null && !phone.isEmpty()) {
            LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
            memberQw.eq(Member::getPhone, phone);
            Member member = memberService.getOne(memberQw);
            if (member != null) {
                qw.eq(RechargeRecord::getMemberId, member.getId());
            } else {
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
            qw.eq(RechargeRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(RechargeRecord::getCreatedTime);
        rechargeRecordService.page(pageInfo, qw);

        List<RechargeRecord> records = pageInfo.getRecords();
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
                .map(RechargeRecord::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
            memberQw.in(Member::getId, memberIds);
            memberQw.select(Member::getId, Member::getName, Member::getPhone);
            List<Member> members = memberService.list(memberQw);
            memberMap = members.stream().collect(Collectors.toMap(Member::getId, m -> m, (a, b) -> a));
        }

        // 组装增强后的记录列表
        List<Map<String, Object>> enhancedRecords = new ArrayList<>();
        for (RechargeRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            Member m = memberMap.get(r.getMemberId());
            item.put("memberName", m != null ? m.getName() : "");
            item.put("phone", m != null ? m.getPhone() : "");
            item.put("amount", r.getAmount());
            item.put("giftAmount", r.getGiftAmount());
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
     * 充值统计
     * 返回全平台充值总览数据：累计总额、今日/本月数据、近12月趋势、支付方式分布
     */
    /**
     * 充值统计
     * @return 全平台充值总览数据（累计总额、今日/本月数据、近12月趋势、支付方式分布）
     */
    @GetMapping("/stats")
    @Operation(summary = "充值统计", description = "获取全平台充值统计数据：累计总额、今日/本月金额与笔数、近12月趋势、支付方式分布")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<RechargeRecord> allQw = new LambdaQueryWrapper<>();
        if (tenantId != null) allQw.eq(RechargeRecord::getTenantId, tenantId);
        List<RechargeRecord> allRecords = rechargeRecordService.list(allQw);

        // 1. 累计充值总额
        BigDecimal totalAmount = allRecords.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCount = allRecords.size();

        // 2. 今日充值
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        List<RechargeRecord> todayRecords = allRecords.stream()
                .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(todayStart) && !r.getCreatedTime().isAfter(todayEnd))
                .collect(Collectors.toList());
        BigDecimal todayAmount = todayRecords.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 本月充值
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        List<RechargeRecord> monthRecords = allRecords.stream()
                .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(monthStart))
                .collect(Collectors.toList());
        BigDecimal monthAmount = monthRecords.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 平均单笔充值金额
        BigDecimal avgAmount = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. 支付方式分布
        Map<String, BigDecimal> paymentMap = allRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getPaymentMethod() != null ? r.getPaymentMethod() : "未知",
                        Collectors.reducing(BigDecimal.ZERO, r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));
        List<Map<String, Object>> paymentDistribution = new ArrayList<>();
        paymentMap.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", k);
            item.put("value", v);
            paymentDistribution.add(item);
        });

        // 6. 近12月趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            LocalDateTime ms = LocalDateTime.of(monthDate.withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime me = LocalDateTime.of(monthDate.withDayOfMonth(monthDate.lengthOfMonth()), LocalTime.MAX);
            BigDecimal monthTotal = allRecords.stream()
                    .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(ms) && !r.getCreatedTime().isAfter(me))
                    .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue()));
            item.put("amount", monthTotal);
            trend.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("totalCount", totalCount);
        result.put("todayAmount", todayAmount);
        result.put("todayCount", todayRecords.size());
        result.put("monthAmount", monthAmount);
        result.put("monthCount", monthRecords.size());
        result.put("avgAmount", avgAmount);
        result.put("paymentDistribution", paymentDistribution);
        result.put("trend", trend);
        return R.success(result);
    }
}
