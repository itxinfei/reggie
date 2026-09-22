package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.inventory.service.MaterialStockService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.groupbuy.service.GroupBuyService;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.reggie.module.payment.model.PaymentOrder.STATUS_FAIL;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_PENDING;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;

/**
 * 支付订单服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements
        PaymentOrderService {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 拼团服务（支付成功后标记拼团参与已支付，幂等，非拼团单自动跳过） */
    @Autowired
    private GroupBuyService groupBuyService;

    /** 退款服务（@Lazy 避免与 RefundServiceImpl 的循环依赖；订单已取消但支付成功时自动退款） */
    @Autowired
    @Lazy
    private RefundService refundService;

    /** 订单明细服务（支付失败回退库存） */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 菜品服务（支付失败回退库存） */
    @Autowired
    private DishService dishService;

    /** 套餐菜品关联服务（支付失败回退套餐内菜品库存） */
    @Autowired
    private SetmealDishService setmealDishService;

    /** 原料库存联动（支付失败按 BOM 恢复原料），可选注入避免循环依赖 */
    @Autowired(required = false)
    private MaterialStockService materialStockService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 分布式锁过期时间（毫秒）：覆盖一次 createPaymentOrder 调用耗时
     */
    private static final long LOCK_TTL_MS = 30 * 1000L; // 30秒

    /**
     * 补偿任务 Redis 幂等 key 前缀（与 {@code StockRefundCompensationTask.compensateKey} 保持一致）
     */
    private static final String STOCK_REFUND_KEY_PREFIX = "stock:refund:";

    /**
     * 补偿任务 Redis 幂等 key TTL（小时），略大于 24h 补偿窗口
     */
    private static final long STOCK_REFUND_KEY_TTL_HOURS = 25;

    /**
     * 创建 payment order。
     * @param orderId 参数 orderId
     * @param channel 参数 channel
     * @param amount 参数 amount
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPaymentOrder(Long orderId, String channel, BigDecimal amount) {
        // 租户归属校验：防止跨租户越权创建支付单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(order.getTenantId())) {
            throw new CustomException("无权对其他租户的订单发起支付");
        }

        // 分布式锁串行化同一订单的并发创建请求，防止 TOCTOU 竞态导致重复 PENDING 支付单（多实例生效）
        String lockKey = "payment:lock:create-order:" + orderId;
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            // Redis 不可用或锁被占用：支付场景优先可用性，降级到 DB 层兜底
            // （countByOrderIdAndStatuses + PENDING 复用）；极端并发可能创建两个 PENDING，回调 CAS 仅一个成功
            log.warn("支付创建分布式锁获取失败，降级 DB 兜底: orderId={}", orderId);
        }
        try {
            // 重复支付检查：同一订单已存在 SUCCESS 支付单则拒绝
            java.util.List<String> statusList = new java.util.ArrayList<>();
            statusList.add(STATUS_SUCCESS);
            int successCount = baseMapper.countByOrderIdAndStatuses(orderId, statusList);
            if (successCount > 0) {
                throw new CustomException("该订单已支付成功，请勿重复支付");
            }
            // 存在 PENDING 支付单则复用返回，避免重复创建支付单和重复调用渠道
            PaymentOrder existPending = lambdaQuery()
                    .eq(PaymentOrder::getOrderId, orderId)
                    .eq(PaymentOrder::getStatus, STATUS_PENDING)
                    .one();
            if (existPending != null) {
                log.info("复用待支付订单: tradeNo={}, orderId={}", existPending.getTradeNo(), orderId);
                return existPending;
            }

            PaymentOrder po = new PaymentOrder();
            po.setOrderId(orderId);
            po.setTenantId(BaseContext.getCurrentTenantId());
            po.setTradeNo(generateTradeNo());
            po.setChannel(channel);
            po.setAmount(amount);
            po.setStatus(STATUS_PENDING);
            save(po);
            log.info("创建支付订单: tradeNo={}, orderId={}, channel={}, amount={}", po.getTradeNo(), orderId, channel,
                    amount);
            return po;
        } finally {
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
    }

    // ──────────────────────────────────────
    // 分布式锁辅助方法（与 OrderTimeoutTask 同模式）
    // ──────────────────────────────────────

    /**
     * 尝试获取分布式锁（支付创建场景）
     * @param lockKey 锁Key
     * @return 锁值（UUID），Redis 不可用或被占用返回 null（降级 DB 兜底）
     */
    private String tryLock(String lockKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("支付创建获取分布式锁失败，降级 DB 兜底: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁（Lua 脚本原子操作：比对锁值后才删除）
     * @param lockKey 锁Key
     * @param lockValue 锁值（UUID）
     */
    private void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("支付创建释放分布式锁失败: {}", lockKey, e);
        }
    }

    /**
     * 查询 by trade no ignore tenant。
     * @param tradeNo 参数 tradeNo
     * @return 返回结果
     */
    @Override
    public PaymentOrder selectByTradeNoIgnoreTenant(String tradeNo) {
        return baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
    }

    /**
     * 处理 payment success。
     * @param tradeNo 参数 tradeNo
     * @param channelTradeNo 参数 channelTradeNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(String tradeNo, String channelTradeNo) {
        // 原子更新：仅当状态为 PENDING 时更新为 SUCCESS，解决回调 read-then-write 竞态（幂等）
        int affected = baseMapper.casUpdateStatus(tradeNo, STATUS_PENDING, STATUS_SUCCESS,
                channelTradeNo, LocalDateTime.now());
        if (affected == 0) {
            // 状态已非 PENDING（已成功/已失败/已退款），幂等跳过
            log.info("支付回调幂等跳过：支付单状态已非PENDING，tradeNo={}", tradeNo);
            return;
        }
        log.info("支付成功: tradeNo={}, channelTradeNo={}", tradeNo, channelTradeNo);

        // 联动更新业务订单状态：仅当订单为待付款(1)时才更新为待接单(2)，避免覆盖已取消/已完成订单（状态机校验）
        PaymentOrder po = baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
        if (po == null) {
            return;
        }
        // 回填租户上下文，确保后续业务订单查询走正确的租户隔离；finally 块清理避免 ThreadLocal 残留
        Long originalTenantId = BaseContext.getCurrentTenantId();
        BaseContext.setCurrentTenantId(po.getTenantId());
        try {
            Orders order = orderService.getById(po.getOrderId());
            if (order != null && order.getStatus() != null) {
                if (Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
                    // 支付成功统一进入待接单/待制作(2)，由商家 confirmOrder 接单(2→3)或 KDS 拉单制作：
                    // ① KDS pullPendingOrders 只拉 STATUS_ORDERED(2)，跳3会导致后厨永远拉不到单；
                    // ② 堂食已付款单标"配送中"语义错误；③ rejectOrder 的自动退款也假设支付成功停在2。
                    // 货到付款(payMethod=6)不走在线支付回调，无影响。
                    int nextStatus = Orders.STATUS_ORDERED;
                    boolean updated = orderService.lambdaUpdate()
                            .eq(Orders::getId, order.getId())
                            .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                            .set(Orders::getStatus, nextStatus)
                            .set(Orders::getCheckoutTime, LocalDateTime.now())
                            .update();
                    if (updated) {
                        log.info("支付成功联动更新订单: orderId={}, orderStatus=待接单",
                                po.getOrderId());
                        // 拼团订单支付成功：标记参与已支付（独立事务+幂等，非拼团单自动跳过；
                        // try-catch 保护支付主流程，拼团标记异常不影响支付状态）
                        try {
                            groupBuyService.markParticipationPaid(po.getOrderId());
                        } catch (Exception ex) {
                            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                            log.warn("拼团参与标记支付失败，跳过: orderId={}, err={}",
                                    po.getOrderId(), ex.getMessage());
                        }
                    } else {
                        log.warn("支付成功但订单状态已被他人变更，跳过联动更新: orderId={}", po.getOrderId());
                    }
                } else {
                    log.warn("支付成功但订单状态非待付款，跳过联动更新: orderId={}, currentStatus={}",
                            po.getOrderId(), order.getStatus());
                    // P0-2 修复（取消/支付 TOCTOU 资金漏洞）：
                    // 此前取消订单的 hasSuccessPaymentOrder 是无锁 count 查询，支付回调并发时可能读到 false，
                    // 订单被 CAS 置为已取消(5)；随后本回调 SUCCESS 后看到订单已取消只 log.warn 跳过，
                    // 形成"payment SUCCESS + order CANCELLED + no refund"的资金矛盾（收钱不退）。
                    // 现于订单为已取消(5)时注册 afterCommit 自动退款，复用 refundService.refundByOrder
                    // （其内部含 Redis 锁 + 幂等校验，重复触发安全）：主事务提交后走渠道全额退款并联动订单为已退款(6)。
                    if (Objects.equals(order.getStatus(), Orders.STATUS_CANCELLED)) {
                        registerAutoRefundOnCancelled(po.getOrderId(), order.getTenantId());
                    }
                }
            }
        } finally {
            if (originalTenantId != null) {
                BaseContext.setCurrentTenantId(originalTenantId);
            } else {
                BaseContext.remove();
            }
        }
    }

    /**
     * 处理 payment fail。
     * @param tradeNo 参数 tradeNo
     * @param errorMsg 参数 errorMsg
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFail(String tradeNo, String errorMsg) {
        // 原子更新：仅当状态为 PENDING 时更新为 FAIL（幂等）
        int affected = baseMapper.casUpdateStatus(tradeNo, STATUS_PENDING, STATUS_FAIL,
                null, LocalDateTime.now());
        if (affected == 0) {
            log.info("支付失败回调幂等跳过：支付单状态已非PENDING，tradeNo={}", tradeNo);
            return;
        }
        log.warn("支付失败: tradeNo={}, errorMsg={}", tradeNo, errorMsg);

        PaymentOrder po = baseMapper.selectByTradeNoIgnoreTenant(tradeNo);
        if (po == null) {
            return;
        }
        // 回填租户上下文，finally 块清理避免 ThreadLocal 残留
        Long originalTenantId = BaseContext.getCurrentTenantId();
        BaseContext.setCurrentTenantId(po.getTenantId());
        try {
            // 仅当订单为待付款时才联动取消，避免覆盖已配送/已完成订单（状态机校验）
            Orders order = orderService.getById(po.getOrderId());
            if (order == null || order.getStatus() == null) {
                return;
            }
            if (!Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
                log.warn("支付失败但订单状态非待付款，跳过联动取消: orderId={}, currentStatus={}",
                        po.getOrderId(), order.getStatus());
                return;
            }
            boolean updated = orderService.lambdaUpdate()
                    .eq(Orders::getId, order.getId())
                    .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                    .set(Orders::getStatus, Orders.STATUS_CANCELLED)
                    .update();
            if (!updated) {
                log.warn("支付失败但订单状态已被他人变更，跳过联动取消: orderId={}", po.getOrderId());
                return;
            }
            log.warn("支付失败联动取消订单: orderId={}, reason={}", po.getOrderId(), errorMsg);
            // P0-3 修复：支付失败已扣库存必须回退，防止库存泄漏
            boolean refundOk = refundStockByOrderId(order.getId());
            if (refundOk) {
                markStockRefunded(order.getId(), order.getTenantId());
                // MEDIUM-3 修复：写入补偿任务的 Redis 幂等 key，防止补偿任务重复 addStock
                // handlePaymentFail 的 markStockRefunded（DB CAS）与补偿任务的 Redis 幂等 key 是两套独立机制，
                // 若 markStockRefunded 因事务回滚未持久化，补偿任务会扫描到 stockRefunded=0 再次补偿
                markCompensatedForOrder(order.getId());
                log.info("[库存回退] 支付失败订单库存已回退: orderId={}", po.getOrderId());
            } else {
                log.error("[库存回退] 支付失败订单库存回退部分失败，补偿任务将重试: orderId={}", po.getOrderId());
            }
        } finally {
            if (originalTenantId != null) {
                BaseContext.setCurrentTenantId(originalTenantId);
            } else {
                BaseContext.remove();
            }
        }
    }

    /**
     * 订单已取消但支付成功：主事务提交后自动发起渠道全额退款（P0-2 TOCTOU 资金闭环）。
     * <p>
     * 复用 {@code refundService.refundByOrder}（其内部含 Redis 分布式锁 + 幂等校验 + 累计退款复查，
     * 重复触发安全）：支付回调并发于取消时，订单已被 CAS 置为已取消(5)，本回调 SUCCESS 后须自动退款，
     * 避免"payment SUCCESS + order CANCELLED + no refund"的资金矛盾（收钱不退）。
     * 注意：此处不回退库存——取消路径已回退过库存，且退款到账与库存回退解耦。
     * </p>
     */
    private void registerAutoRefundOnCancelled(Long orderId, Long tenantId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 处理 after commit。
             */
            @Override
            public void afterCommit() {
                try {
                    boolean refunded = refundService.refundByOrder(orderId, "订单已取消支付成功自动退款");
                    if (refunded) {
                        log.info("订单已取消但支付成功，自动退款完成: orderId={}", orderId);
                    } else {
                        log.warn("订单已取消但支付成功，自动退款未完成（幂等跳过或已留对账待办）: orderId={}", orderId);
                    }
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.error("【严重】订单已取消但支付成功，自动退款异常，需人工处理！orderId={}, tenantId={}",
                            orderId, tenantId, e);
                }
            }
        });
    }

    /**
     * 标记库存已回退（与 OrderStatusFlowServiceImpl.markStockRefunded 一致）
     */
    private void markStockRefunded(Long orderId, Long tenantId) {
        orderService.lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_CANCELLED)
                .eq(Orders::getStockRefunded, 0)
                .set(Orders::getStockRefunded, 1)
                .update();
    }

    /**
     * 根据订单ID回退库存（与 OrderStatusFlowServiceImpl.refundStockByOrderId 逻辑一致）
     * 查询订单明细，逐项回退菜品/套餐库存。
     * @return 是否全部回退成功
     */
    private boolean refundStockByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details == null || details.isEmpty()) {
            return true;
        }
        boolean allSuccess = true;
        for (OrderDetail detail : details) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);
            if (!refundStockForItem(detail.getDishId(), detail.getSetmealId(), qty)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    /**
     * 回退单个订单明细项的库存（单品/套餐）
     */
    private boolean refundStockForItem(Long dishId, Long setmealId, BigDecimal quantity) {
        boolean success = true;
        if (dishId != null) {
            if (!refundStockAtomic(dishId, quantity)) {
                success = false;
            }
        }
        if (setmealId != null) {
            LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
            sdWrapper.eq(SetmealDish::getSetmealId, setmealId);
            List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
            for (SetmealDish sd : setmealDishes) {
                int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                if (!refundStockAtomic(sd.getDishId(), quantity.multiply(new BigDecimal(copies)))) {
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * 原子增加菜品库存（boolean 版本，失败时记录日志但不抛异常）
     */
    private boolean refundStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        try {
            dishService.addStock(dishId, qty);
            dishService.autoToggleSoldOut(dishId);
            log.info("[库存回退] 菜品ID={} 回退{}份", dishId, qty);
            // 同步按 BOM 恢复原料（与 OrderStockRefundServiceImpl 口径一致）；
            // 原料恢复失败仅告警、不改变菜品回退结果，避免两套实现导致原料库存持续泄漏
            if (materialStockService != null) {
                try {
                    materialStockService.restoreMaterialStock(dishId, qty);
                } catch (Exception me) {
                    log.error("[库存回退] 菜品ID={} 原料恢复失败，需人工核查: {}",
                            dishId, me.getMessage(), me);
                }
            }
            return true;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[库存回退失败] 菜品ID={} 回退{}份失败: {}", dishId, qty, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 写入补偿任务的 Redis 幂等 key，防止 {@code StockRefundCompensationTask} 重复 addStock。
     * <p>
     * handlePaymentFail 的 {@code markStockRefunded}（DB CAS stockRefunded 0→1）与补偿任务的 Redis 幂等 key
     * 是两套独立的幂等机制，互不可见。若 DB 标记因事务回滚等原因未持久化，补偿任务会扫描到 stockRefunded=0 再次回退库存。
     * 本方法在 DB 标记成功后，同步写入补偿任务的 Redis key，让补偿任务的 {@code isCompensated} 检查能识别已补偿过。
     * </p>
     * <p>
     * 仅标记有实际库存回退的明细项（DishId/SetmealId 非空），与补偿任务的 {@code compensateOrderStock} 逻辑对齐。
     * key 格式：stock:refund:{orderId}:{detailId}:{subKey}，TTL 25h，与 StockRefundCompensationTask 一致。
     * </p>
     */
    private void markCompensatedForOrder(Long orderId) {
        if (redisTemplate == null) {
            return;
        }
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details == null || details.isEmpty()) {
            return;
        }
        for (OrderDetail detail : details) {
            // 单品菜品
            if (detail.getDishId() != null) {
                String subKey = "dish:" + detail.getDishId();
                setCompensateKey(orderId, detail.getId(), subKey);
            }
            // 套餐：标记套餐内所有菜品子项
            if (detail.getSetmealId() != null) {
                LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
                sdWrapper.eq(SetmealDish::getSetmealId, detail.getSetmealId());
                List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
                for (SetmealDish sd : setmealDishes) {
                    String subKey = "sd:" + sd.getId();
                    setCompensateKey(orderId, detail.getId(), subKey);
                }
            }
        }
    }

    /**
     * 设置补偿幂等 Redis key（TTL 25h）
     */
    private void setCompensateKey(Long orderId, Long detailId, String subKey) {
        try {
            String key = STOCK_REFUND_KEY_PREFIX + orderId + ":" + detailId + ":" + subKey;
            redisTemplate.opsForValue().set(key, "1", STOCK_REFUND_KEY_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.debug("[库存回退] 写入补偿幂等 key 异常（不影响主流程）: {}", e.getMessage());
        }
    }

    private String generateTradeNo() {
        // 使用UUID保证交易号唯一性，避免Math.random()的并发问题
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}


