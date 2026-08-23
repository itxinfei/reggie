package com.reggie.module.order.service.statusflow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.event.OrderCancelledEvent;
import com.reggie.common.event.OrderCompletedEvent;
import com.reggie.enums.OrderStatus;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 订单状态流转服务实现类
 *
 * 从 {@link com.reggie.module.order.service.impl.OrderServiceImpl} 中提取，
 * 职责：状态变更 + 拒单/取消的库存回退与权益回退 + 事件发布。
 *
 * 库存扣减（下单路径）仍保留在 OrderServiceImpl 中，本服务仅处理回退路径。
 *
 * @author reggie
 * @since 2026-08-22
 */
@Service
@Slf4j
public class OrderStatusFlowServiceImpl
        extends ServiceImpl<OrderMapper, Orders>
        implements OrderStatusFlowService {

    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 菜品服务（库存回退） */
    @Autowired
    private DishService dishService;

    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    /** 会员权益服务（拒单/取消时回退积分与券） */
    @Autowired
    private MemberRewardService memberRewardService;

    /** Spring 事件发布器（订单完成/取消事件） */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ==================== 状态流转入口 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, Long id) {
        if (status == null || id == null) {
            throw new CustomException("参数缺失，无法更新订单状态");
        }
        // 修复状态机跳跃：按目标状态复用合法流转方法，禁止任意跳转
        if (Objects.equals(status, Orders.STATUS_DELIVERING)) {
            confirmOrder(id);
        } else if (Objects.equals(status, Orders.STATUS_COMPLETED)) {
            completeOrder(id);
        } else if (Objects.equals(status, Orders.STATUS_CANCELLED)) {
            cancelOrder(id, null);
        } else {
            throw new CustomException("非法的目标状态：" + getStatusName(status)
                    + "，仅支持流转为配送中(3)/已完成(4)/已取消(5)，请通过专用接口操作");
        }
    }

    /**
     * 接单：待接单(2) → 配送中(3)
     * <p>
     * 偏安全默认：使用数据库行级条件更新（WHERE id=? AND status=期望值）保证原子性，
     * 避免 read→check→updateById 在并发下的竞态（两个并发请求同时读到同一状态后都判定可接单，
     * 导致状态被错误覆盖或触发下游副作用两次）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long id) {
        Orders order = atomicUpdateStatusIf(id, Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING);
        log.info("订单已接单: id={}, number={}", id, order != null ? order.getNumber() : null);
    }

    /**
     * 拒单：待接单(2) → 已取消(5)，同时回退库存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long id) {
        Orders order = atomicUpdateStatusIf(id, Orders.STATUS_ORDERED, Orders.STATUS_CANCELLED);
        if (order == null) {
            throw new CustomException("订单状态不正确，无法拒单");
        }

        // 拒单时回退库存（部分失败也允许，补偿任务会重试）
        boolean refundOk = refundStockByOrderId(id);
        if (refundOk) {
            markStockRefunded(id, order.getTenantId());
            log.warn("订单已拒单（库存已回退）: id={}, number={}", id, order.getNumber());
        } else {
            log.error("订单已拒单，但库存回退部分失败，补偿任务将重试: id={}, number={}", id, order.getNumber());
        }

        // 拒单时回退会员权益（积分、已核销优惠券），失败不影响主流程日志
        try {
            memberRewardService.reverseRewards(id, order.getTenantId());
        } catch (Exception e) {
            log.error("[会员权益] 订单{}拒单后权益回退失败，需人工核查: {}", id, e.getMessage(), e);
        }
    }

    /**
     * 完成订单：配送中(3) → 已完成(4)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long id) {
        Orders order = atomicUpdateStatusComplete(id);
        if (order == null) {
            throw new CustomException("订单状态不正确，无法完成");
        }

        // 发布订单完成事件（推荐、积分等模块异步响应）
        // 注意：事件发布放在事务外由 Spring 保证（publishEvent 默认同步，
        // 事务提交前已发布，监听器 @Async 异步拾取），与原子状态更新的 ordering 由数据库 + 事件顺序保证
        eventPublisher.publishEvent(new OrderCompletedEvent(this, id, order.getTenantId()));
        log.info("订单已完成并触发后续事件: id={}, number={}", id, order.getNumber());
    }

    /**
     * 取消订单：任意非完成/取消状态 → 已取消(5)，同时回退库存
     * <p>
     * 偏安全默认：取消接口原本允许从任意"非完成/取消"状态转入取消，为避免"待接单被并发拒单"与"取消被并发确认"
     * 的竞态，取消同样采用行级条件更新：在 WHERE 中排除已完成(4)和已取消(5)，其余状态均可转入取消。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        // 租户归属校验：防止跨租户越权取消订单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !Objects.equals(currentTenantId, order.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        Integer curStatus = order.getStatus();
        if (Objects.equals(curStatus, Orders.STATUS_COMPLETED)) {
            throw new CustomException("订单已完成，无法取消");
        }
        if (Objects.equals(curStatus, Orders.STATUS_CANCELLED)) {
            throw new CustomException("订单已取消，无需重复操作");
        }

        // 行级条件更新：从当前状态进入已取消（防止并发下被接单/拒单覆盖）
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .eq(Orders::getStatus, curStatus)
                .set(Orders::getStatus, Orders.STATUS_CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            wrapper.set(Orders::getRemark, reason);
        }
        boolean rows = this.update(null, wrapper);
        if (!rows) {
            throw new CustomException("订单状态已变更，无法取消，请刷新后重试");
        }

        // 取消订单时回退库存（部分失败也允许，补偿任务会重试）
        boolean refundOk = refundStockByOrderId(id);
        if (refundOk) {
            markStockRefunded(id, order.getTenantId());
            log.warn("订单已取消（库存已回退）: id={}, number={}, reason={}", id, order.getNumber(), reason);
        } else {
            log.error("订单已取消，但库存回退部分失败，补偿任务将重试: id={}, number={}, reason={}", id, order.getNumber(), reason);
        }

        // 取消订单时回退会员权益（积分、已核销优惠券），失败不影响主流程日志
        try {
            memberRewardService.reverseRewards(id, order.getTenantId());
        } catch (Exception e) {
            log.error("[会员权益] 订单{}取消后权益回退失败，需人工核查: {}", id, e.getMessage(), e);
        }

        // 发布订单取消事件（通知、推荐等模块异步响应）
        eventPublisher.publishEvent(new OrderCancelledEvent(this, id, order.getTenantId(), reason));
    }

    // ==================== 状态名称 ====================

    /**
     * 订单状态中文名称（委托给 {@link OrderStatus} 枚举）
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        OrderStatus orderStatus = OrderStatus.fromCode(status);
        return orderStatus != null ? orderStatus.getDesc() : "其他(" + status + ")";
    }

    // ==================== 原子状态更新（行级条件更新，防并发竞态） ====================

    /**
     * 原子更新订单状态：WHERE id=? AND status=expectedStatus → SET status=targetStatus。
     * 返回影响到的订单（若非空则更新成功）；若影响行数为 0（订单不存在或状态不符），返回 null。
     *
     * @param id             订单 ID
     * @param expectedStatus 期望的当前状态
     * @param targetStatus   目标状态
     */
    private Orders atomicUpdateStatusIf(Long id, Integer expectedStatus, Integer targetStatus) {
        Orders existing = this.getById(id);
        if (existing == null) {
            throw new CustomException("订单不存在");
        }
        // 租户归属校验：防止跨租户越权修改订单状态
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !Objects.equals(currentTenantId, existing.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        if (!Objects.equals(existing.getStatus(), expectedStatus)) {
            throw new CustomException("订单状态不正确，当前状态：" + getStatusName(existing.getStatus())
                    + "，无法执行该操作");
        }
        // 行级条件更新：数据库层面保证同一时刻只有一个请求能命中
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .eq(Orders::getStatus, expectedStatus)
                .set(Orders::getStatus, targetStatus);
        boolean rows = this.update(null, wrapper);
        if (!rows) {
            throw new CustomException("订单状态已变更，请刷新后重试");
        }
        // 返回最新订单供调用方取 number/tenantId 等字段
        return this.getById(id);
    }

    /**
     * 完成订单的原子状态更新：配送中(3) → 已完成(4)，同时设置结账时间。
     * <p>
     * checkoutTime 在实体上标记了 INSERT_UPDATE 自动填充，MyMetaObjectHandler 会注入；
     * 此处显式 set 一个确定值覆盖，确保结账时间为"本方法调用时"，与业务语义一致。
     */
    private Orders atomicUpdateStatusComplete(Long id) {
        Orders existing = this.getById(id);
        if (existing == null) {
            throw new CustomException("订单不存在");
        }
        // 租户归属校验：防止跨租户越权完成订单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !Objects.equals(currentTenantId, existing.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        if (!Objects.equals(existing.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单状态不正确，当前状态：" + getStatusName(existing.getStatus())
                    + "，无法完成");
        }
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .eq(Orders::getStatus, Orders.STATUS_DELIVERING)
                .set(Orders::getStatus, Orders.STATUS_COMPLETED)
                .set(Orders::getCheckoutTime, LocalDateTime.now());
        boolean rows = this.update(null, wrapper);
        if (!rows) {
            throw new CustomException("订单状态已变更，无法完成，请刷新后重试");
        }
        return this.getById(id);
    }

    /**
     * 标记库存已回退（拒单/取消共用）
     */
    private void markStockRefunded(Long id, Long tenantId) {
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .eq(Orders::getStatus, Orders.STATUS_CANCELLED)
                .set(Orders::getStockRefunded, 1);
        this.update(null, wrapper);
    }

    // ==================== 库存回退（仅回退路径；扣减路径保留在 OrderServiceImpl） ====================

    /**
     * 库存操作函数式接口
     * @return 操作是否成功
     */
    private interface StockOperation {
        boolean apply(Long dishId, BigDecimal qty);
    }

    /**
     * 处理菜品/套餐的库存操作（扣减或回退）
     */
    private boolean processStockForItems(Long dishId, Long setmealId, BigDecimal quantity, StockOperation operation) {
        boolean success = true;

        // 单品菜品
        if (dishId != null) {
            if (!operation.apply(dishId, quantity)) {
                success = false;
            }
        }

        // 套餐：处理套餐内所有菜品
        if (setmealId != null) {
            LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
            sdWrapper.eq(SetmealDish::getSetmealId, setmealId);
            List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
            for (SetmealDish sd : setmealDishes) {
                int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                if (!operation.apply(sd.getDishId(), quantity.multiply(new BigDecimal(copies)))) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * 使用乐观锁原子扣减菜品库存
     * WHERE stock_qty >= qty，防止并发超卖
     */
    private void deductStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        dishService.deductStock(dishId, qty);
        dishService.autoToggleSoldOut(dishId);
    }

    /**
     * 回退库存原子操作（boolean 版本，失败时记录日志但不抛异常）
     * @return 是否成功
     */
    private boolean refundStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        try {
            dishService.addStock(dishId, qty);
            dishService.autoToggleSoldOut(dishId);
            log.info("[库存回退] 菜品ID={} 回退{}份", dishId, qty);
            return true;
        } catch (Exception e) {
            log.error("[库存回退失败] 菜品ID={} 回退{}份失败: {}", dishId, qty, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据订单ID回退库存
     * 查询订单明细，逐项回退菜品/套餐库存
     * @return 是否全部回退成功
     */
    private boolean refundStockByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details != null && !details.isEmpty()) {
            return refundStockForOrderDetails(details);
        }
        return true; // 无明细视为成功
    }

    /**
     * 回退库存操作（订单明细维度）
     */
    private boolean refundStockForOrderDetails(List<OrderDetail> orderDetails) {
        boolean allSuccess = true;
        for (OrderDetail detail : orderDetails) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);
            if (!processStockForItems(detail.getDishId(), detail.getSetmealId(), qty, this::refundStockAtomic)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }
}