package com.reggie.module.member.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员积分记录，支持按手机号搜索，返回关联会员信息（名称、手机号、余额）")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "phone", description = "会员手机号（可选，精确查询）")
    public R<Map<String, Object>> page(int page, int pageSize, String phone) {
        Page<PointsRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PointsRecord> qw = new LambdaQueryWrapper<>();

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
}
