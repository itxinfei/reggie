package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.enums.MemberBizTag;
import com.reggie.entity.Orders;
import com.reggie.module.member.mapper.MemberTagMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberTag;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.MemberTagService;
import com.reggie.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    public List<MemberTag> listByTagType(Long tenantId, String tagType) {
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getTagType, tagType);
        qw.orderByDesc(MemberTag::getCreateTime);
        return this.list(qw);
    }

    @Override
    public List<MemberTag> listByBizTag(Long tenantId, String bizTag) {
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getBizTag, bizTag);
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

        LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
        memberQw.eq(Member::getTenantId, tenantId);
        memberQw.eq(Member::getStatus, 1);
        List<Member> members = memberService.list(memberQw);

        for (Member member : members) {
            Long memberId = member.getId();
            Long userId = member.getUserId();

            // 查询近90天内的订单
            LocalDateTime ninetyDaysAgo = now.minusDays(90);
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.eq(Orders::getUserId, userId);
            orderQw.ge(Orders::getOrderTime, ninetyDaysAgo);
            orderQw.ne(Orders::getStatus, Orders.STATUS_CANCELLED);
            orderQw.ne(Orders::getStatus, Orders.STATUS_REFUNDED);
            List<Orders> recentOrders = orderMapper.selectList(orderQw);

            // 查询所有订单（用于总消费）
            LambdaQueryWrapper<Orders> allOrderQw = new LambdaQueryWrapper<>();
            allOrderQw.eq(Orders::getUserId, userId);
            allOrderQw.ne(Orders::getStatus, Orders.STATUS_CANCELLED);
            allOrderQw.ne(Orders::getStatus, Orders.STATUS_REFUNDED);
            List<Orders> allOrders = orderMapper.selectList(allOrderQw);

            BigDecimal totalConsumption = allOrders.stream()
                    .map(Orders::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 近30天订单数
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
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
