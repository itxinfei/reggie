package com.reggie.module.order.service.statusflow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.event.OrderCancelledEvent;
import com.reggie.common.event.OrderCompletedEvent;
import com.reggie.enums.DiningTableStatus;
import com.reggie.enums.OrderSource;
import com.reggie.enums.OrderStatus;
import com.reggie.module.inventory.service.MaterialStockService;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundService;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    /** 骑手在途单量上限，与派单"可用骑手 currentOrderCount<3"口径保持一致 */
    private static final int MAX_RIDER_LOAD = 3;

    /** 取餐码长度（P0-6 核销） */
    private static final int PICKUP_CODE_LENGTH = 6;

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

    /** 支付单服务（已支付订单取消/拒单时检测与联动） */
    @Autowired
    private PaymentOrderService paymentOrderService;

    /** 退款服务（已支付订单取消/拒单自动全额退款，资金闭环） */
    @Autowired
    private RefundService refundService;

    /** 桌台服务（堂食订单完成/取消/拒单时释放桌台占用） */
    @Autowired
    private com.reggie.module.dining.service.DiningTableService diningTableService;

    /** 原料库存联动服务（可选注入，退款时按 BOM 恢复原料） */
    @Autowired(required = false)
    private MaterialStockService materialStockService;

    @Autowired(required = false)
    private com.reggie.module.marketing.service.MarketingToolService marketingToolService;

    /** 配送跟踪服务（骑手信息、负载计数、接单/取餐/送达时间戳） */
    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    /** 骑手结算服务（配送完成入账，与会员余额解耦） */
    @Autowired(required = false)
    private com.reggie.module.delivery.service.RiderSettlementService riderSettlementService;

    /** 骑手消息服务（派单/改派提醒，失败不阻塞主流程） */
    @Autowired(required = false)
    private com.reggie.module.delivery.service.RiderMessageService riderMessageService;

    /**
     * 写入骑手消息（宽异常兜底：有意捕获 Exception，消息写入失败不得阻塞订单主流程）。
     */
    private void sendRiderMessage(Long riderId, Long tenantId, int type,
                                  String title, String content, Long bizId) {
        if (riderMessageService == null || riderId == null) {
            return;
        }
        try {
            riderMessageService.send(riderId, tenantId, type, title, content, bizId);
        } catch (Exception e) {
            log.error("[骑手消息] 写入失败，不阻塞主流程: riderId={}, bizId={}, msg={}",
                    riderId, bizId, e.getMessage(), e);
        }
    }

    /**
     * 持久化取消原因（P0-5 回执）：写入独立字段 cancel_reason，供顾客端展示，不覆盖下单备注。
     */
    private void persistCancelReason(Long orderId, String reason) {
        if (orderId == null || reason == null || reason.trim().isEmpty()) {
            return;
        }
        this.lambdaUpdate()
                .eq(Orders::getId, orderId)
                .set(Orders::getCancelReason, reason)
                .update();
    }

    // ==================== 状态流转入口 ====================

    /**
     * 更新 status。
     * @param status 参数 status
     * @param id 参数 id
     */
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
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        // 租户归属校验：防止跨租户越权拒单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !Objects.equals(currentTenantId, order.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单状态不正确，无法拒单");
        }

        // 已支付订单拒单（支付成功会把订单 1→2，待接单可能已付款）→ 自动退款，不置已取消
        if (hasSuccessPaymentOrder(id)) {
            // 修改点(P0-5)：落拒单原因供顾客端回执
            persistCancelReason(id, "商家拒单");
            registerAutoRefund(id, "商家拒单自动退款", order.getTenantId());
            log.warn("订单拒单（已支付，自动退款）: id={}, number={}", id, order.getNumber());
            return;
        }

        // 未支付拒单：待接单(2) → 已取消(5)，同时回退库存
        Orders rejected = atomicUpdateStatusIf(id, Orders.STATUS_ORDERED, Orders.STATUS_CANCELLED);
        if (rejected == null) {
            throw new CustomException("订单状态不正确，无法拒单");
        }
        // 修改点(P0-5)：拒单原因回执（独立字段，不覆盖顾客下单备注）
        persistCancelReason(id, "商家拒单");

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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[会员权益] 订单{}拒单后权益回退失败，需人工核查: {}", id, e.getMessage(), e);
        }

        // 堂食订单拒单 → 释放桌台（主事务内同步执行，失败不阻塞主流程）
        releaseTableIfEatIn(order);
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
        // 堂食订单完成 → 释放桌台（主事务内同步执行，失败不阻塞主流程）
        releaseTableIfEatIn(order);
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

        // 已支付订单取消（支付成功会把订单 1→2，配送中同样可能已付款）→ 自动退款，不置已取消
        // 订单状态由退款服务在渠道退款成功后联动为已退款(6)，避免"已取消但已付款"的资金矛盾
        if (hasSuccessPaymentOrder(id)) {
            // 修改点(P0-5)：已支付取消不置已取消，但仍需落取消原因供顾客端回执
            persistCancelReason(id, reason);
            registerAutoRefund(id, reason, order.getTenantId());
            eventPublisher.publishEvent(new OrderCancelledEvent(this, id, order.getTenantId(), reason));
            log.warn("订单已取消（已支付，自动退款）: id={}, number={}, reason={}", id, order.getNumber(), reason);
            return;
        }

        // 行级条件更新：从当前状态进入已取消（防止并发下被接单/拒单覆盖）
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .eq(Orders::getStatus, curStatus)
                .set(Orders::getStatus, Orders.STATUS_CANCELLED);
        // 修改点(P0-5)：取消原因写入独立字段 cancel_reason，不再覆盖顾客下单备注 remark
        if (reason != null && !reason.trim().isEmpty()) {
            wrapper.set(Orders::getCancelReason, reason);
        }
        boolean rows = this.update(versionGuardEntity(order.getVersion()), wrapper);
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[会员权益] 订单{}取消后权益回退失败，需人工核查: {}", id, e.getMessage(), e);
        }

        // 取消订单时释放秒杀名额（活动库存 + 限购额度）。
        // 与 recordFlashSaleUsage 严格对称：下单同时占用两者，只回退库存会让"下单→取消"
        // 永久占用名额、限购永远拦住后续购买。失败不影响主流程，交由补偿任务重试。
        try {
            if (marketingToolService != null) {
                marketingToolService.releaseFlashSaleUsage(id, order.getTenantId());
            }
        } catch (Exception e) {
            // 宽异常兜底：与会员权益回退同口径，避免单个失败阻断取消主流程
            log.error("[秒杀] 订单{}取消后名额回退失败，需人工核查: {}", id, e.getMessage(), e);
        }

        // 堂食订单取消 → 释放桌台（主事务内同步执行，失败不阻塞主流程）
        releaseTableIfEatIn(order);

        // 发布订单取消事件（通知、推荐等模块异步响应）
        eventPublisher.publishEvent(new OrderCancelledEvent(this, id, order.getTenantId(), reason));
    }

    // ==================== 骑手派单 / 抢单 / 取餐 / 送达 ====================

    /**
     * 店长派单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchOrder(Long orderId, Long riderId) {
        if (riderId == null) {
            throw new CustomException("请选择骑手");
        }
        Orders order = requireOrderInTenant(orderId);
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法派单");
        }
        if (order.getRiderId() != null) {
            throw new CustomException("该订单已指派骑手");
        }
        Rider rider = requireRiderInTenant(riderId, order.getTenantId());
        if (Objects.equals(rider.getStatus(), Rider.STATUS_OFFLINE)) {
            throw new CustomException("骑手当前离线，无法派单");
        }

        // 行级条件：status=2 且未指派，保证并发派单只有一次命中
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_ORDERED)
                .isNull(Orders::getRiderId)
                .set(Orders::getRiderId, riderId)
                .set(Orders::getDispatchTime, LocalDateTime.now());
        if (!this.update(null, wrapper)) {
            throw new CustomException("订单已被处理，请刷新后重试");
        }
        // 修改点(P0-6)：派单即生成取餐码，供骑手到店核销
        ensurePickupCode(order);
        // 修改点(P0-4)：派单即向骑手推送待接单提醒
        sendRiderMessage(riderId, order.getTenantId(),
                com.reggie.module.delivery.model.RiderMessage.TYPE_DISPATCH,
                "新订单待接单",
                "订单 " + order.getNumber() + " 已派给你，请尽快接单取餐", orderId);
        log.info("订单已派单：orderId={}, riderId={}", orderId, riderId);
    }

    /**
     * 骑手抢单（抢 + 接单一步完成）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grabOrder(Long orderId, Long riderId) {
        Orders order = requireOrderInTenant(orderId);
        Rider rider = requireRiderInTenant(riderId, order.getTenantId());
        if (Objects.equals(rider.getStatus(), Rider.STATUS_OFFLINE)) {
            throw new CustomException("请先上线后再抢单");
        }
        int riderLoad = rider.getCurrentOrderCount() == null ? 0 : rider.getCurrentOrderCount();
        if (riderLoad >= MAX_RIDER_LOAD) {
            throw new CustomException("当前在途订单已达上限，请完成配送后再抢单");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED) || order.getRiderId() != null) {
            throw new CustomException("订单已被抢走或状态已变更");
        }

        // CAS：条件 status=2 且 rider_id IS NULL，set 骑手与 status=3；影响行数=1 才算成功
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_ORDERED)
                .isNull(Orders::getRiderId)
                .set(Orders::getRiderId, riderId)
                .set(Orders::getDispatchTime, LocalDateTime.now())
                .set(Orders::getStatus, Orders.STATUS_DELIVERING);
        if (!this.update(null, wrapper)) {
            throw new CustomException("手慢了，订单已被其他骑手抢走");
        }

        // 修改点(P0-6)：抢单即生成取餐码，供骑手到店核销
        ensurePickupCode(order);
        afterRiderAccepted(order, rider);
        // P0-4：抢单成功也推送一条消息，便于骑手在消息页留痕与回溯（店长派单走 dispatchOrder 已发）
        sendRiderMessage(riderId, order.getTenantId(),
                com.reggie.module.delivery.model.RiderMessage.TYPE_DISPATCH,
                "派单成功",
                "您已抢到订单 " + order.getNumber() + "，请尽快到店取餐",
                orderId);
        log.info("骑手抢单成功：orderId={}, riderId={}", orderId, riderId);
    }

    /**
     * 骑手确认派单（店长已指派，骑手接单）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptRiderTask(Long orderId, Long riderId) {
        Orders order = requireOrderInTenant(orderId);
        Rider rider = requireRiderInTenant(riderId, order.getTenantId());
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法接单");
        }
        if (!Objects.equals(order.getRiderId(), riderId)) {
            throw new CustomException("该订单未指派给你");
        }

        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_ORDERED)
                .eq(Orders::getRiderId, riderId)
                .set(Orders::getStatus, Orders.STATUS_DELIVERING);
        if (!this.update(null, wrapper)) {
            throw new CustomException("订单状态已变更，请刷新后重试");
        }

        afterRiderAccepted(order, rider);
        log.info("骑手确认派单：orderId={}, riderId={}", orderId, riderId);
    }

    /**
     * 骑手确认取餐（主状态仍为配送中，记录取餐时间）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickupRiderTask(Long orderId, Long riderId, String pickupCode) {
        Orders order = requireOrderInTenant(orderId);
        if (!Objects.equals(order.getRiderId(), riderId)) {
            throw new CustomException("该订单不属于你");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法确认取餐");
        }
        // 修改点(P0-6)：取餐码核销——已生成取餐码的订单必须校验一致，
        // 防止未到店即点取餐；历史无码订单放行，避免存量数据被卡住。
        String expected = order.getPickupCode();
        if (expected != null && !expected.trim().isEmpty()) {
            if (pickupCode == null || !expected.trim().equals(pickupCode.trim())) {
                throw new CustomException("取餐码不正确，请向店员索取正确的取餐码");
            }
        }
        deliveryTrackingService.recordRiderAction(orderId, order.getNumber(), order.getOrderTime(),
                riderId, riderNameOf(riderId), DeliveryTrackingService.ACTION_PICKUP);
        log.info("骑手已取餐：orderId={}, riderId={}", orderId, riderId);
    }

    /**
     * 店员核销自提订单：核对取餐码后完成（3 → 4）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifySelfPickupOrder(Long orderId, String pickupCode) {
        Orders order = requireOrderInTenant(orderId);
        if (!Objects.equals(OrderSource.SELF_PICKUP.getValue(), order.getSource())) {
            throw new CustomException("该订单不是自提订单");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法核销");
        }
        String expected = order.getPickupCode();
        if (expected == null || expected.trim().isEmpty()) {
            // 正常不会出现：自提下单即生成码；兜底直接拒绝，避免无码单被任意核销
            throw new CustomException("订单缺少取餐码，请刷新后重试");
        }
        if (pickupCode == null || !expected.trim().equals(pickupCode.trim())) {
            throw new CustomException("取餐码不正确，请与顾客核对");
        }
        // 复用完成逻辑：3 → 4 并发完成事件（积分等）
        completeOrder(orderId);
        log.info("自提订单已核销完成：orderId={}", orderId);
    }

    /**
     * 生成并持久化取餐码（仅当订单尚无取餐码时）。用于派单 / 抢单环节，
     * 保证骑手到店前门店已持有一个可核销的取餐码。
     *
     * @param order 订单（已加载）
     * @return 订单当前有效的取餐码
     */
    private String ensurePickupCode(Orders order) {
        String existing = order.getPickupCode();
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }
        String code = cn.hutool.core.util.RandomUtil.randomNumbers(PICKUP_CODE_LENGTH);
        this.lambdaUpdate()
                .eq(Orders::getId, order.getId())
                .set(Orders::getPickupCode, code)
                .update();
        return code;
    }

    /**
     * 骑手确认送达（3 → 4）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverRiderOrder(Long orderId, Long riderId) {
        Orders order = requireOrderInTenant(orderId);
        if (!Objects.equals(order.getRiderId(), riderId)) {
            throw new CustomException("该订单不属于你");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法确认送达");
        }

        Rider rider = requireRiderInTenant(riderId, order.getTenantId());
        // 记录送达时间戳 → 完成订单（3→4，发 OrderCompletedEvent）→ 骑手负载 -1
        deliveryTrackingService.recordRiderAction(orderId, order.getNumber(), order.getOrderTime(),
                riderId, rider.getName(), DeliveryTrackingService.ACTION_DELIVER);
        this.completeOrder(orderId);
        changeRiderLoad(rider, -1);
        // 修改点：配送完成即按配送费入账到骑手独立账户（幂等，与会员余额解耦）
        if (riderSettlementService != null) {
            riderSettlementService.settle(orderId, riderId, order.getTenantId(), order.getDeliveryFee());
        }
        log.info("骑手已送达：orderId={}, riderId={}", orderId, riderId);
    }

    /**
     * 订单改派：在途订单（status 2 已指派 或 3 配送中）由原骑手转给目标骑手。
     * <p>
     * 使用行级条件更新（rider_id=原骑手 且 status in (2,3)）保证并发下仅一次改派命中；
     * 成功后原子调整双方在途单量（原骑手 -1、目标骑手 +1），由底层 SQL 维护在线/忙碌状态。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignRiderOrder(Long orderId, Long oldRiderId, Long newRiderId) {
        if (newRiderId == null) {
            throw new CustomException("请选择目标骑手");
        }
        Orders order = requireOrderInTenant(orderId);
        if (order.getRiderId() == null) {
            throw new CustomException("订单尚未指派骑手，无法改派");
        }
        if (Objects.equals(order.getRiderId(), newRiderId)) {
            throw new CustomException("目标骑手与原骑手相同，无需改派");
        }
        if (oldRiderId != null && !Objects.equals(order.getRiderId(), oldRiderId)) {
            throw new CustomException("订单已不属于原骑手，改派失败");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)
                && !Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单当前为" + getStatusName(order.getStatus()) + "，无法改派");
        }
        Rider newRider = requireRiderInTenant(newRiderId, order.getTenantId());
        if (Objects.equals(newRider.getStatus(), Rider.STATUS_OFFLINE)) {
            throw new CustomException("目标骑手当前离线，无法改派");
        }
        int newLoad = newRider.getCurrentOrderCount() == null ? 0 : newRider.getCurrentOrderCount();
        if (newLoad >= MAX_RIDER_LOAD) {
            throw new CustomException("目标骑手在途订单已达上限，无法改派");
        }

        Long fromRiderId = order.getRiderId();
        // 行级条件：rider_id=原骑手 且 status in (2,3)，防并发重复改派
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, orderId)
                .eq(Orders::getRiderId, fromRiderId)
                .in(Orders::getStatus, Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING)
                .set(Orders::getRiderId, newRiderId)
                .set(Orders::getDispatchTime, LocalDateTime.now());
        if (!this.update(null, wrapper)) {
            throw new CustomException("订单已被处理，改派失败，请刷新后重试");
        }

        // 同步在途单量：原骑手释放在途（不计入累计完成），目标骑手在途 +1
        deliveryTrackingService.releaseRiderLoad(fromRiderId);
        deliveryTrackingService.adjustRiderLoad(newRiderId, 1);
        // 修改点(P0-4)：改派/转单后通知目标骑手与原骑手
        sendRiderMessage(newRiderId, order.getTenantId(),
                com.reggie.module.delivery.model.RiderMessage.TYPE_DISPATCH,
                "新订单转给你",
                "订单 " + order.getNumber() + " 已转给你，请尽快配送", orderId);
        sendRiderMessage(fromRiderId, order.getTenantId(),
                com.reggie.module.delivery.model.RiderMessage.TYPE_OTHER,
                "订单已转出",
                "订单 " + order.getNumber() + " 已转给其他骑手", orderId);
        log.info("订单改派：orderId={}, fromRider={}, toRider={}", orderId, fromRiderId, newRiderId);
    }

    // ---- 骑手流程私有辅助 ----

    /**
     * 接单成功后的统一副作用：骑手负载 +1、记录接单时间戳。
     */
    private void afterRiderAccepted(Orders order, Rider rider) {
        changeRiderLoad(rider, 1);
        deliveryTrackingService.recordRiderAction(order.getId(), order.getNumber(), order.getOrderTime(),
                rider.getId(), rider.getName(), DeliveryTrackingService.ACTION_ACCEPT);
    }

    /**
     * 取订单并做存在性与租户归属校验。
     */
    private Orders requireOrderInTenant(Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !Objects.equals(currentTenantId, order.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }
        return order;
    }

    /**
     * 取骑手并校验其属于订单所在租户。
     */
    private Rider requireRiderInTenant(Long riderId, Long tenantId) {
        Rider rider = deliveryTrackingService.getRiderById(riderId);
        if (rider == null) {
            throw new CustomException("骑手不存在");
        }
        if (tenantId != null && !Objects.equals(tenantId, rider.getTenantId())) {
            throw new CustomException("骑手不属于当前门店");
        }
        return rider;
    }

    private String riderNameOf(Long riderId) {
        Rider r = deliveryTrackingService.getRiderById(riderId);
        return r == null ? null : r.getName();
    }

    /**
     * 维护骑手在途单量与状态。
     * delta=+1 接单：在途 +1，在线 → 忙碌；
     * delta=-1 送达：在途 -1、累计单量 +1，在途归零且原忙碌 → 回到在线。
     */
    private void changeRiderLoad(Rider rider, int delta) {
        // 在途计数与在线/忙碌状态由底层原子 SQL 维护，避免读-改-写并发丢失更新
        deliveryTrackingService.adjustRiderLoad(rider.getId(), delta);
    }

    // ==================== 堂食桌台释放 ====================

    /**
     * 堂食订单释放桌台：订单完成/取消/拒单时，将桌台状态释放为空闲。
     * <p>
     * 以 try/catch 包裹，桌台释放失败不阻塞订单主流程（异常日志需人工核查）。
     * 注意：{@link DiningTableService#changeStatus} 为 fail-closed（要求租户上下文），
     * 必须在主事务内同步调用，不能放入 afterCommit 异步回调（那时 BaseContext 已被清理）。
     * </p>
     */
    private void releaseTableIfEatIn(Orders order) {
        if (order == null || order.getTableId() == null
                || !Objects.equals(order.getSource(), OrderSource.EAT_IN.getValue())) {
            return;
        }
        try {
            diningTableService.changeStatus(order.getTableId(), DiningTableStatus.FREE.getValue());
            log.info("[桌台释放] 订单{}释放桌台{}为空闲", order.getId(), order.getTableId());
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[桌台释放失败] 订单{}桌台{}释放异常，需人工核查: {}",
                    order.getId(), order.getTableId(), e.getMessage(), e);
        }
    }

    // ==================== 已支付订单自动退款（资金闭环） ====================

    /**
     * 是否已存在成功支付的支付单（已支付订单取消/拒单时必须自动退款，禁止"收钱后订单直接取消"）。
     */
    private boolean hasSuccessPaymentOrder(Long id) {
        return paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getOrderId, id)
                .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_SUCCESS)
                .count() > 0;
    }

    /**
     * 已支付订单取消/拒单：主事务提交后自动发起渠道全额退款（外部 HTTP 不放事务内）。
     * <p>
     * 时序保证（先退钱再还库存）：
     * 1. 主事务提交后调用 {@link RefundService#refundByOrder} 走渠道全额退款，成功则支付单置 REFUND、
     *    订单 CAS 联动为已退款(6)（订单 2/3/4 → 6），并统一回退会员权益；
     * 2. 退款成功后再回退库存（避免"钱未退、库存先还"造成超卖）；
     * 3. 任一环节失败记录红色告警，需人工核查（资金对账兜底）。
     * </p>
     *
     * @param id       订单ID
     * @param reason   退款原因
     * @param tenantId 租户ID（库存回退标记用）
     */
    private void registerAutoRefund(Long id, String reason, Long tenantId) {
        final String fReason = (reason != null && !reason.trim().isEmpty()) ? reason : "订单取消自动退款";
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 处理 after commit。
             */
            @Override
            public void afterCommit() {
                boolean refunded = false;
                try {
                    refunded = refundService.refundByOrder(id, fReason);
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.error("【严重】订单取消后自动退款异常，需人工处理！orderId={}", id, e);
                }
                if (refunded) {
                    try {
                        boolean stockOk = refundStockByOrderId(id);
                        if (stockOk) {
                            markStockRefunded(id, tenantId);
                        } else {
                            log.error("订单自动退款成功但库存回退部分失败，补偿任务将重试: orderId={}", id);
                        }
                    } catch (Exception e) {
                        // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                        log.error("订单自动退款成功但库存回退异常，需人工核查: orderId={}", id, e);
                    }
                    // 退款成功后释放秒杀名额（活动库存 + 限购额度），与库存回退同口径
                    try {
                        if (marketingToolService != null) {
                            marketingToolService.releaseFlashSaleUsage(id, tenantId);
                        }
                    } catch (Exception e) {
                        log.error("订单{}自动退款后秒杀名额回退异常，需人工核查: {}", id, e.getMessage(), e);
                    }
                }
            }
        });
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
     * 版本守卫：仅当加载到的 {@code version} 非空时返回携带版本号的更新实体，
     * 使 MyBatis-Plus 注入乐观锁条件（WHERE version=? 且 SET version=version+1）；
     * 若版本为 null（历史数据），返回 null 以回退到无版本更新，避免 {@code WHERE version=null} 全不匹配。
     * <p>
     * 修复(缺口5/FOCUS_REVIEW)：{@code Orders} 标注了 {@code @Version} 但全仓更新均传 {@code entity=null}，
     * 导致乐观锁形同虚设；本方法让状态机热路径在常态下真正生效乐观锁，且不破坏历史 null 行。
     */
    private Orders versionGuardEntity(Integer version) {
        if (version == null) {
            return null;
        }
        Orders entity = new Orders();
        entity.setVersion(version);
        return entity;
    }

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
        boolean rows = this.update(versionGuardEntity(existing.getVersion()), wrapper);
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
        boolean rows = this.update(versionGuardEntity(existing.getVersion()), wrapper);
        if (!rows) {
            throw new CustomException("订单状态已变更，无法完成，请刷新后重试");
        }
        return this.getById(id);
    }

    /**
     * 标记库存已回退（拒单/取消共用）
     */
    private void markStockRefunded(Long id, Long tenantId) {
        // 修复（2026-09-17 冒烟）：已支付订单取消/拒单走自动退款后订单状态为「已退款(6)」而非「已取消(5)」，
        // 旧逻辑 WHERE status=CANCELLED 永不命中 → stock_refunded 标记置位失败，
        // 导致 StockRefundCompensationTask 每 30 分钟把已退款订单库存再回退一次（重复恢复、库存膨胀）。
        // 现同时匹配 CANCELLED 与 REFUNDED 两种终态，并加 ne(stock_refunded,1) 幂等保护。
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, id)
                .in(Orders::getStatus, Orders.STATUS_CANCELLED, Orders.STATUS_REFUNDED)
                .ne(Orders::getStockRefunded, 1)
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
            // 原料库存联动：按 BOM 配方同步恢复原料
            if (materialStockService != null) {
                materialStockService.restoreMaterialStock(dishId, qty);
            }
            log.info("[库存回退] 菜品ID={} 回退{}份", dishId, qty);
            return true;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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