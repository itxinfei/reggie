package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.enums.MemberBizTag;
import com.reggie.module.order.model.Orders;
import com.reggie.module.member.mapper.MemberTagMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberTag;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.MemberTagService;
import com.reggie.module.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会员标签服务实现类
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
@Service
public class MemberTagServiceImpl extends ServiceImpl<MemberTagMapper, MemberTag> implements MemberTagService {

    @Autowired
    private MemberService memberService;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public List<MemberTag> listByMemberId(Long tenantId, Long memberId) {
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getMemberId, memberId);
        qw.orderByDesc(MemberTag::getCreateTime);
        return this.list(qw);
    }

    @Override
    public Map<String, Long> countByBizTag(Long tenantId) {
        List<Map<String, Object>> result = this.baseMapper.countByBizTag(tenantId);
        Map<String, Long> countMap = new LinkedHashMap<>();
        if (result != null) {
            for (Map<String, Object> row : result) {
                String bizTag = (String) row.get("bizTag");
                Long count = (Long) row.get("count");
                if (bizTag != null) {
                    countMap.put(bizTag, count != null ? count : 0L);
                }
            }
        }
        return countMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTag(Long tenantId, Long memberId, String tagName, String bizTag, String tagColor) {
        MemberTag memberTag = new MemberTag();
        memberTag.setTenantId(tenantId);
        memberTag.setMemberId(memberId);
        memberTag.setTagName(tagName);
        memberTag.setBizTag(bizTag);
        memberTag.setTagColor(tagColor);
        memberTag.setTagType(1);
        memberTag.setCreateTime(LocalDateTime.now());
        memberTag.setCreateUser(BaseContext.getCurrentId());
        return this.save(memberTag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveTags(Long tenantId, Long memberId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getMemberId, memberId);
        qw.in(MemberTag::getId, tagIds);
        return this.remove(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateTags(Long tenantId) {
        log.info("开始自动生成会员标签，tenantId={}", tenantId);
        int generatedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        // 1. 批量查询租户下所有活跃会员
        LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
        memberQw.eq(Member::getTenantId, tenantId);
        memberQw.eq(Member::getStatus, 1);
        List<Member> members = memberService.list(memberQw);
        if (members.isEmpty()) {
            return 0;
        }

        // 2. 批量查询所有活跃会员的订单（避免N+1）
        List<Long> userIds = members.stream()
                .map(Member::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LocalDateTime ninetyDaysAgo = now.minusDays(90);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Map<Long, List<Orders>> recentOrdersByUser = Collections.emptyMap();
        Map<Long, List<Orders>> allOrdersByUser = Collections.emptyMap();
        Map<Long, BigDecimal> totalConsumptionByUser = Collections.emptyMap();

        if (!userIds.isEmpty()) {
            // 批量查询近90天订单（排除取消/退款）
            LambdaQueryWrapper<Orders> recentQw = new LambdaQueryWrapper<>();
            recentQw.eq(Orders::getTenantId, tenantId);
            recentQw.in(Orders::getUserId, userIds);
            recentQw.ge(Orders::getOrderTime, ninetyDaysAgo);
            recentQw.ne(Orders::getStatus, Orders.STATUS_CANCELLED);
            recentQw.ne(Orders::getStatus, Orders.STATUS_REFUNDED);
            List<Orders> recentOrders = orderMapper.selectList(recentQw);
            recentOrdersByUser = recentOrders.stream()
                    .collect(Collectors.groupingBy(Orders::getUserId));

            // 批量查询所有有效订单
            LambdaQueryWrapper<Orders> allQw = new LambdaQueryWrapper<>();
            allQw.eq(Orders::getTenantId, tenantId);
            allQw.in(Orders::getUserId, userIds);
            allQw.ne(Orders::getStatus, Orders.STATUS_CANCELLED);
            allQw.ne(Orders::getStatus, Orders.STATUS_REFUNDED);
            List<Orders> allOrders = orderMapper.selectList(allQw);
            allOrdersByUser = allOrders.stream()
                    .collect(Collectors.groupingBy(Orders::getUserId));

            // 预计算每个用户的总消费
            totalConsumptionByUser = allOrdersByUser.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .map(Orders::getAmount)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    ));
        }

        // 3. 遍历会员，使用预查询的数据生成标签
        for (Member member : members) {
            Long memberId = member.getId();
            Long userId = member.getUserId();
            if (userId == null) continue;

            List<Orders> recentOrders = recentOrdersByUser.getOrDefault(userId, Collections.emptyList());
            List<Orders> allOrders = allOrdersByUser.getOrDefault(userId, Collections.emptyList());
            BigDecimal totalConsumption = totalConsumptionByUser.getOrDefault(userId, BigDecimal.ZERO);
            long recentOrderCount = recentOrders.stream()
                    .filter(o -> o.getOrderTime() != null && o.getOrderTime().isAfter(thirtyDaysAgo))
                    .count();

            // 注册7天内无消费 → NEW_USER
            if (member.getCreatedTime() != null &&
                    member.getCreatedTime().plusDays(7).isAfter(now) &&
                    allOrders.isEmpty()) {
                if (addTagIfNotExist(tenantId, memberId, "新用户", MemberBizTag.NEW_USER.getValue(), "#909399")) {
                    generatedCount++;
                }
            }

            // 近90天无消费 → LAPSED
            boolean hasRecentOrder = recentOrders.stream()
                    .anyMatch(o -> o.getOrderTime() != null && o.getOrderTime().isAfter(ninetyDaysAgo));
            if (!hasRecentOrder && !allOrders.isEmpty()) {
                if (addTagIfNotExist(tenantId, memberId, "流失预警", MemberBizTag.LAPSED.getValue(), "#E6A23C")) {
                    generatedCount++;
                }
            }

            // 总消费 > 500 元 → HIGH_VALUE
            if (totalConsumption.compareTo(new BigDecimal("500")) > 0) {
                if (addTagIfNotExist(tenantId, memberId, "高价值客户", MemberBizTag.HIGH_VALUE.getValue(), "#F56C6C")) {
                    generatedCount++;
                }
            }

            // 近30天订单数 > 5 → HIGHLY_ACTIVE
            if (recentOrderCount > 5) {
                if (addTagIfNotExist(tenantId, memberId, "高活跃", MemberBizTag.HIGHLY_ACTIVE.getValue(), "#409EFF")) {
                    generatedCount++;
                }
            }

            // 积分余额 > 2000 → FOODIE
            if (member.getPoints() != null && member.getPoints() > 2000) {
                if (addTagIfNotExist(tenantId, memberId, "美食家", MemberBizTag.FOODIE.getValue(), "#67C23A")) {
                    generatedCount++;
                }
            }
        }

        log.info("自动生成会员标签完成，共生成{}个标签", generatedCount);
        return generatedCount;
    }

    /**
     * 如果标签不存在则添加
     */
    private boolean addTagIfNotExist(Long tenantId, Long memberId, String tagName, String bizTag, String tagColor) {
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getMemberId, memberId);
        qw.eq(MemberTag::getBizTag, bizTag);
        if (this.count(qw) > 0) {
            return false;
        }
        return addTag(tenantId, memberId, tagName, bizTag, tagColor);
    }
}



