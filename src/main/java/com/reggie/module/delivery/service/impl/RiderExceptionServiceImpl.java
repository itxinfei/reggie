package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.delivery.mapper.RiderExceptionOrderMapper;
import com.reggie.module.delivery.model.RiderExceptionOrder;
import com.reggie.module.delivery.model.RiderMessage;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.delivery.service.RiderExceptionService;
import com.reggie.module.delivery.service.RiderMessageService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 骑手异常工单服务实现。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@Service
public class RiderExceptionServiceImpl implements RiderExceptionService {

    @Autowired
    private RiderExceptionOrderMapper exceptionMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    @Autowired
    private OrderStatusFlowService orderStatusFlowService;

    @Autowired(required = false)
    private RiderMessageService riderMessageService;

    private static boolean validType(Integer type) {
        return type != null && type >= 1 && type <= 6;
    }

    /**
     * 通知上报骑手工单处理结果（宽异常兜底：消息失败不影响工单处理）。
     */
    private void notifyRiderHandleResult(RiderExceptionOrder e, String action) {
        if (riderMessageService == null || e == null) {
            return;
        }
        String title;
        String content;
        if ("REASSIGN".equals(action)) {
            title = "异常已处理：订单已改派";
            content = "你上报的订单 " + e.getOrderNumber() + " 异常已由后台改派给其他骑手";
        } else if ("RESOLVE".equals(action)) {
            title = "异常已处理";
            content = "你上报的订单 " + e.getOrderNumber() + " 异常已处理完成";
        } else {
            title = "异常工单已关闭";
            content = "你上报的订单 " + e.getOrderNumber() + " 异常工单已关闭";
        }
        if (org.springframework.util.StringUtils.hasText(e.getHandleNote())) {
            content = content + "，处理意见：" + e.getHandleNote();
        }
        try {
            riderMessageService.send(e.getRiderId(), e.getTenantId(),
                    RiderMessage.TYPE_EXCEPTION, title, content, e.getOrderId());
        } catch (Exception ex) {
            log.error("[骑手消息] 异常结果通知失败，不影响工单处理: id={}, msg={}",
                    e.getId(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderExceptionOrder submit(Long riderId, Long tenantId, Long orderId,
                                     Integer exceptionType, String description) {
        if (!validType(exceptionType)) {
            throw new CustomException("异常的异常类型");
        }
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (tenantId != null && !Objects.equals(tenantId, order.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        if (!Objects.equals(order.getRiderId(), riderId)) {
            throw new CustomException("该订单不属于你，无法上报异常");
        }
        // 仅配送中（已接单/取餐）可上报异常；已完成/取消/待接单（未接）不允许
        if (!Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)
                && !Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单当前状态不可上报异常");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new CustomException("请填写异常描述");
        }

        RiderExceptionOrder e = new RiderExceptionOrder();
        e.setTenantId(tenantId);
        e.setOrderId(orderId);
        e.setOrderNumber(order.getNumber());
        e.setRiderId(riderId);
        e.setRiderName(deliveryTrackingService.getRiderById(riderId) == null
                ? null : deliveryTrackingService.getRiderById(riderId).getName());
        e.setExceptionType(exceptionType);
        e.setDescription(description.trim());
        e.setStatus(RiderExceptionOrder.STATUS_PENDING);
        exceptionMapper.insert(e);
        log.info("骑手上报异常：orderId={}, riderId={}, type={}", orderId, riderId, exceptionType);
        return e;
    }

    @Override
    public Page<RiderExceptionOrder> pageMine(Long riderId, Long tenantId, int page, int size) {
        Page<RiderExceptionOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<RiderExceptionOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderExceptionOrder::getRiderId, riderId)
                .eq(RiderExceptionOrder::getTenantId, tenantId)
                .eq(RiderExceptionOrder::getIsDeleted, 0)
                .orderByDesc(RiderExceptionOrder::getCreateTime);
        return exceptionMapper.selectPage(p, qw);
    }

    @Override
    public Page<RiderExceptionOrder> adminPage(Long tenantId, Integer exceptionType, Integer status,
                                              String orderNumber, int page, int size) {
        Page<RiderExceptionOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<RiderExceptionOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderExceptionOrder::getTenantId, tenantId)
                .eq(RiderExceptionOrder::getIsDeleted, 0);
        if (exceptionType != null) {
            qw.eq(RiderExceptionOrder::getExceptionType, exceptionType);
        }
        if (status != null) {
            qw.eq(RiderExceptionOrder::getStatus, status);
        }
        if (StringUtils.hasText(orderNumber)) {
            qw.like(RiderExceptionOrder::getOrderNumber, orderNumber.trim());
        }
        qw.orderByDesc(RiderExceptionOrder::getCreateTime);
        return exceptionMapper.selectPage(p, qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderExceptionOrder handle(Long id, Long tenantId, Long operatorId, String action,
                                     String handleNote, Long newRiderId) {
        RiderExceptionOrder e = exceptionMapper.selectById(id);
        if (e == null || Objects.equals(e.getIsDeleted(), 1)) {
            throw new CustomException("工单不存在");
        }
        if (tenantId != null && !Objects.equals(tenantId, e.getTenantId())) {
            throw new CustomException("无权处理其他租户的工单");
        }
        if (!Objects.equals(e.getStatus(), RiderExceptionOrder.STATUS_PENDING)) {
            throw new CustomException("工单已处理，请勿重复操作");
        }
        if (!StringUtils.hasText(action)) {
            throw new CustomException("请指定处理动作");
        }
        if ("REASSIGN".equals(action)) {
            if (newRiderId == null) {
                throw new CustomException("改派需指定目标骑手");
            }
            // 改派：将订单从原上报骑手转给目标骑手（同步调整在途单量与轨迹）
            orderStatusFlowService.reassignRiderOrder(e.getOrderId(), e.getRiderId(), newRiderId);
            e.setStatus(RiderExceptionOrder.STATUS_TRANSFERRED);
        } else if ("RESOLVE".equals(action)) {
            e.setStatus(RiderExceptionOrder.STATUS_HANDLED);
        } else if ("CLOSE".equals(action)) {
            e.setStatus(RiderExceptionOrder.STATUS_CLOSED);
        } else {
            throw new CustomException("未知的处理动作：" + action);
        }
        e.setHandleNote(handleNote);
        e.setHandlerId(operatorId);
        e.setHandleTime(LocalDateTime.now());
        exceptionMapper.updateById(e);
        // 修改点(P0-4)：处理结果回执给上报骑手
        notifyRiderHandleResult(e, action);
        log.info("骑手异常工单处理：id={}, action={}, operatorId={}", id, action, operatorId);
        return e;
    }

    @Override
    public Map<Integer, Long> countByStatus(Long tenantId) {
        LambdaQueryWrapper<RiderExceptionOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderExceptionOrder::getTenantId, tenantId)
                .eq(RiderExceptionOrder::getIsDeleted, 0);
        List<RiderExceptionOrder> all = exceptionMapper.selectList(qw);
        Map<Integer, Long> map = new HashMap<>();
        for (RiderExceptionOrder e : all) {
            Integer s = e.getStatus();
            map.put(s, map.getOrDefault(s, 0L) + 1);
        }
        return map;
    }
}
