package com.reggie.module.kds.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.kds.mapper.KitchenTicketMapper;
import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.kds.service.KitchenTicketService;
import com.reggie.module.kds.vo.KitchenBoardVO;
import com.reggie.module.kds.vo.KitchenTicketVO;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后厨出餐（KDS）服务实现：从订单幂等拉单生成工单，驱动制作状态流转并聚合大屏看板。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Slf4j
@Service
public class KitchenTicketServiceImpl
        extends ServiceImpl<KitchenTicketMapper, KitchenTicket>
        implements KitchenTicketService {

    /** 菜品摘要最大长度 */
    private static final int SUMMARY_MAX_LEN = 200;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pullPendingOrders() {
        Long tenantId = requireTenant();
        // 1. 查当前租户"已下单/待接单"的订单（支付完成、等待后厨制作）
        List<Orders> pendingOrders = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.STATUS_ORDERED)
                .orderByAsc(Orders::getId));
        if (pendingOrders.isEmpty()) {
            return 0;
        }
        List<Long> orderIds = pendingOrders.stream().map(Orders::getId).collect(Collectors.toList());

        // 2. 排除已生成过工单的订单（幂等）
        List<KitchenTicket> existed = list(new LambdaQueryWrapper<KitchenTicket>()
                .in(KitchenTicket::getOrderId, orderIds)
                .select(KitchenTicket::getOrderId));
        java.util.Set<Long> existedOrderIds = existed.stream()
                .map(KitchenTicket::getOrderId).collect(Collectors.toSet());

        List<Orders> newOrders = pendingOrders.stream()
                .filter(o -> !existedOrderIds.contains(o.getId()))
                .collect(Collectors.toList());
        if (newOrders.isEmpty()) {
            return 0;
        }

        // 3. 一次性查询新订单的明细并按订单分组，避免 N+1
        List<Long> newOrderIds = newOrders.stream().map(Orders::getId).collect(Collectors.toList());
        Map<Long, List<OrderDetail>> detailMap = loadDetails(newOrderIds);

        // 4. 生成工单
        LocalDateTime now = LocalDateTime.now();
        List<KitchenTicket> tickets = new ArrayList<>();
        for (Orders order : newOrders) {
            KitchenTicket ticket = new KitchenTicket();
            ticket.setTenantId(tenantId);
            ticket.setOrderId(order.getId());
            ticket.setOrderNo(order.getNumber());
            ticket.setOrderType(order.getSource());
            ticket.setTableName(order.getTableName());
            ticket.setCustomerCount(order.getCustomerCount());
            ticket.setStatus(KitchenTicket.STATUS_PENDING);
            ticket.setUrgent(KitchenTicket.URGENT_NO);
            ticket.setDishSummary(buildSummary(detailMap.get(order.getId())));
            ticket.setReceiveTime(now);
            ticket.setRemark(order.getRemark());
            tickets.add(ticket);
        }
        saveBatch(tickets);
        log.info("[后厨KDS] 拉取新订单生成工单 {} 张，tenant={}", tickets.size(), tenantId);
        return tickets.size();
    }

    @Override
    public KitchenBoardVO getBoard(boolean autoPull) {
        KitchenBoardVO board = new KitchenBoardVO();
        if (autoPull) {
            board.setPulledCount(pullPendingOrders());
        }
        LocalDateTime now = LocalDateTime.now();

        // 在制工单：待制作/制作中/待取餐
        List<KitchenTicket> active = list(new LambdaQueryWrapper<KitchenTicket>()
                .in(KitchenTicket::getStatus,
                        KitchenTicket.STATUS_PENDING,
                        KitchenTicket.STATUS_COOKING,
                        KitchenTicket.STATUS_READY)
                .orderByAsc(KitchenTicket::getReceiveTime));
        Map<Long, List<OrderDetail>> detailMap = loadDetails(
                active.stream().map(KitchenTicket::getOrderId).collect(Collectors.toList()));

        List<Long> cookDurations = new ArrayList<>();
        int urgent = 0;
        for (KitchenTicket t : active) {
            KitchenTicketVO vo = toVO(t, detailMap.get(t.getOrderId()), now);
            if (t.getStatus() == KitchenTicket.STATUS_PENDING) {
                board.getPending().add(vo);
                if (KitchenTicket.URGENT_YES == (t.getUrgent() == null ? 0 : t.getUrgent())) { urgent++; }
            } else if (t.getStatus() == KitchenTicket.STATUS_COOKING) {
                board.getCooking().add(vo);
                if (KitchenTicket.URGENT_YES == (t.getUrgent() == null ? 0 : t.getUrgent())) { urgent++; }
            } else {
                board.getReady().add(vo);
                if (t.getCookDurationSeconds() != null) { cookDurations.add(t.getCookDurationSeconds()); }
            }
        }

        // 今日已完成数量与平均制作耗时
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        List<KitchenTicket> finishedToday = list(new LambdaQueryWrapper<KitchenTicket>()
                .eq(KitchenTicket::getStatus, KitchenTicket.STATUS_FINISHED)
                .ge(KitchenTicket::getFinishTime, dayStart));
        for (KitchenTicket t : finishedToday) {
            if (t.getCookDurationSeconds() != null) { cookDurations.add(t.getCookDurationSeconds()); }
        }
        board.setFinishedCount(finishedToday.size());
        board.setPendingCount(board.getPending().size());
        board.setCookingCount(board.getCooking().size());
        board.setReadyCount(board.getReady().size());
        board.setUrgentCount(urgent);
        board.setAvgCookSeconds(cookDurations.isEmpty() ? 0L
                : Math.round(cookDurations.stream().mapToLong(Long::longValue).average().orElse(0L)));
        return board;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitchenTicket startCook(Long id) {
        KitchenTicket ticket = getOwned(id);
        if (!Integer.valueOf(KitchenTicket.STATUS_PENDING).equals(ticket.getStatus())) {
            throw new CustomException("仅待制作工单可开始制作");
        }
        ticket.setStatus(KitchenTicket.STATUS_COOKING);
        ticket.setCookStartTime(LocalDateTime.now());
        updateById(ticket);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitchenTicket markReady(Long id) {
        KitchenTicket ticket = getOwned(id);
        if (!Integer.valueOf(KitchenTicket.STATUS_COOKING).equals(ticket.getStatus())) {
            throw new CustomException("仅制作中工单可叫号出餐");
        }
        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(KitchenTicket.STATUS_READY);
        ticket.setReadyTime(now);
        if (ticket.getCookStartTime() != null) {
            ticket.setCookDurationSeconds(Duration.between(ticket.getCookStartTime(), now).getSeconds());
        }
        updateById(ticket);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitchenTicket finish(Long id) {
        KitchenTicket ticket = getOwned(id);
        if (!Integer.valueOf(KitchenTicket.STATUS_READY).equals(ticket.getStatus())) {
            throw new CustomException("仅待取餐工单可确认出餐完成");
        }
        ticket.setStatus(KitchenTicket.STATUS_FINISHED);
        ticket.setFinishTime(LocalDateTime.now());
        updateById(ticket);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitchenTicket cancel(Long id) {
        KitchenTicket ticket = getOwned(id);
        int s = ticket.getStatus() == null ? 0 : ticket.getStatus();
        if (s != KitchenTicket.STATUS_PENDING && s != KitchenTicket.STATUS_COOKING) {
            throw new CustomException("仅待制作/制作中工单可取消");
        }
        ticket.setStatus(KitchenTicket.STATUS_CANCELLED);
        ticket.setCancelTime(LocalDateTime.now());
        updateById(ticket);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KitchenTicket toggleUrgent(Long id) {
        KitchenTicket ticket = getOwned(id);
        int s = ticket.getStatus() == null ? 0 : ticket.getStatus();
        if (s == KitchenTicket.STATUS_FINISHED || s == KitchenTicket.STATUS_CANCELLED) {
            throw new CustomException("已完结工单不能设置加急");
        }
        int cur = ticket.getUrgent() == null ? KitchenTicket.URGENT_NO : ticket.getUrgent();
        ticket.setUrgent(cur == KitchenTicket.URGENT_YES ? KitchenTicket.URGENT_NO : KitchenTicket.URGENT_YES);
        updateById(ticket);
        return ticket;
    }

    @Override
    public Page<KitchenTicket> pageTickets(int page, int pageSize, Integer status) {
        requireTenant();
        Page<KitchenTicket> p = PageUtils.of(page, pageSize);
        return lambdaQuery()
                .eq(status != null, KitchenTicket::getStatus, status)
                .orderByDesc(KitchenTicket::getId)
                .page(p);
    }

    // ============================ 私有辅助 ============================

    private Long requireTenant() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文缺失，无法操作后厨工单");
        }
        return tenantId;
    }

    private KitchenTicket getOwned(Long id) {
        if (id == null) {
            throw new CustomException("工单ID不能为空");
        }
        requireTenant();
        KitchenTicket ticket = getById(id);
        if (ticket == null) {
            throw new CustomException("后厨工单不存在");
        }
        return ticket;
    }

    /**
     * 批量加载订单明细并按订单ID分组（空入参直接返回空 Map，避免 IN () 语法错误）。
     */
    private Map<Long, List<OrderDetail>> loadDetails(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .in(OrderDetail::getOrderId, orderIds)
                .orderByAsc(OrderDetail::getId));
        Map<Long, List<OrderDetail>> map = new HashMap<>();
        for (OrderDetail d : details) {
            map.computeIfAbsent(d.getOrderId(), k -> new ArrayList<>()).add(d);
        }
        return map;
    }

    /**
     * 拼接菜品摘要：名称(口味)x数量；名称x数量，多个以中文分号分隔并截断。
     */
    private String buildSummary(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderDetail d : details) {
            if (sb.length() > 0) { sb.append("；"); }
            sb.append(d.getName());
            if (d.getDishFlavor() != null && !d.getDishFlavor().trim().isEmpty()) {
                sb.append("(").append(d.getDishFlavor().trim()).append(")");
            }
            sb.append("x").append(d.getNumber() == null ? 1 : d.getNumber());
            if (sb.length() >= SUMMARY_MAX_LEN) { break; }
        }
        String s = sb.toString();
        return s.length() > SUMMARY_MAX_LEN ? s.substring(0, SUMMARY_MAX_LEN) : s;
    }

    /**
     * 实体转 VO，填明细与等待时长。
     */
    private KitchenTicketVO toVO(KitchenTicket t, List<OrderDetail> details, LocalDateTime now) {
        KitchenTicketVO vo = new KitchenTicketVO();
        // 复制工单本体字段
        vo.setId(t.getId());
        vo.setTenantId(t.getTenantId());
        vo.setOrderId(t.getOrderId());
        vo.setOrderNo(t.getOrderNo());
        vo.setOrderType(t.getOrderType());
        vo.setTableName(t.getTableName());
        vo.setCustomerCount(t.getCustomerCount());
        vo.setStatus(t.getStatus());
        vo.setUrgent(t.getUrgent());
        vo.setDishSummary(t.getDishSummary());
        vo.setReceiveTime(t.getReceiveTime());
        vo.setCookStartTime(t.getCookStartTime());
        vo.setReadyTime(t.getReadyTime());
        vo.setFinishTime(t.getFinishTime());
        vo.setCancelTime(t.getCancelTime());
        vo.setCookDurationSeconds(t.getCookDurationSeconds());
        vo.setRemark(t.getRemark());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        vo.setDetails(details == null ? Collections.emptyList() : details);
        // 等待时长：已叫号及以后算到叫号，在制算到当前
        LocalDateTime end = t.getReadyTime() != null ? t.getReadyTime() : now;
        if (t.getReceiveTime() != null) {
            long sec = Duration.between(t.getReceiveTime(), end).getSeconds();
            vo.setWaitSeconds(Math.max(sec, 0L));
        } else {
            vo.setWaitSeconds(0L);
        }
        return vo;
    }
}
