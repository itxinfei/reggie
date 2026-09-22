package com.reggie.module.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.cashier.model.DailySettlement;
import com.reggie.module.order.model.Orders;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.cashier.mapper.DailySettlementMapper;
import com.reggie.module.cashier.mapper.CashierRecordMapper;
import com.reggie.module.cashier.service.CashierService;
import com.reggie.module.member.model.CouponAvailableDTO;
import com.reggie.module.member.service.CouponUserService;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.cost.service.CostService;
import com.reggie.module.cost.model.DishCost;
import com.reggie.module.payment.service.PaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.reggie.common.event.OrderCompletedEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import com.reggie.enums.DiningTableStatus;
import com.reggie.enums.OrderSource;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.MemberLevelService;
import com.reggie.module.member.service.MemberService;

/**
 * 收银服务实现
 *
 * @author reggie
 * @since 2026-08-10
 */
@Slf4j
@Service
public class CashierServiceImpl extends ServiceImpl<CashierRecordMapper, CashierRecord> implements CashierService {

    @Autowired
    private CashierRecordMapper cashierRecordMapper;

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private CostService costService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private MemberRewardService memberRewardService;

    @Autowired
    private CouponUserService couponUserService;

    /**
     * 桌台服务（修改点 2026-09-18）：堂食结账完成后需释放桌台，否则结账闭环断裂
     * （订单已收款但桌台仍占用、可重复结账）。可选依赖，避免与 dining 模块循环注入问题。
     */
    @Autowired(required = false)
    private DiningTableService diningTableService;

    /** 会员服务（会员等级折扣 / 券归属会员主键反查） */
    @Autowired(required = false)
    private MemberService memberService;

    /** 会员等级服务（等级折扣率） */
    @Autowired(required = false)
    private MemberLevelService memberLevelService;

    /**
     * 收银支付幂等 Redis 模板（可选依赖，Redis 不可用时跳过幂等检查）
     */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 收银支付幂等 key 前缀
     */
    private static final String CASHIER_IDEMPOTENCY_KEY_PREFIX = "cashier:idempotency:";

    /**
     * 收银支付幂等锁过期时间（秒），足够覆盖同一笔订单的重复提交窗口
     */
    private static final long CASHIER_IDEMPOTENCY_TTL_SECONDS = 3600;

    /**
     * 按 tenantId+date 串行化日结请求，防止并发重复日结（TOCTOU）
     */
    private final ConcurrentHashMap<String, Object> settlementLock = new ConcurrentHashMap<>();

    // ==================== 收银记录管理 ====================

    /**
     * 获取 cashier record list。
     * @param payType 参数 payType
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<CashierRecord> getCashierRecordList(Integer payType, LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId) {
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        if (payType != null) {
            qw.eq(CashierRecord::getPayType, payType);
        }
        if (startDate != null) {
            qw.ge(CashierRecord::getCashierTime, startDate);
        }
        if (endDate != null) {
            qw.le(CashierRecord::getCashierTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(CashierRecord::getCashierTime);
        return cashierRecordMapper.selectList(qw);
    }

    /**
     * 获取 cashier record by order id。
     * @param orderId 参数 orderId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public CashierRecord getCashierRecordByOrderId(Long orderId, Long tenantId) {
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CashierRecord::getOrderId, orderId);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        return cashierRecordMapper.selectOne(qw);
    }

    /**
     * 保存 cashier record。
     * @param cashierRecord 参数 cashierRecord
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCashierRecord(CashierRecord cashierRecord) {
        cashierRecord.setCreateTime(LocalDateTime.now());
        return cashierRecordMapper.insert(cashierRecord) > 0;
    }

    /**
     * 处理 cash payment。
     * @param orderId 参数 orderId
     * @param orderNumber 参数 orderNumber
     * @param amount 参数 amount
     * @param actualAmount 参数 actualAmount
     * @param payType 参数 payType
     * @param cashierId 参数 cashierId
     * @param cashierName 参数 cashierName
     * @param usedCouponId 参数 usedCouponId
     * @param memberUserId 参数 memberUserId
     * @param remark 参数 remark
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashierRecord cashPayment(Long orderId, String orderNumber, BigDecimal amount, BigDecimal actualAmount,
                                     Integer payType, Long cashierId, String cashierName,
                                     Long usedCouponId, Long memberUserId, String remark) {
        payType = (payType == null || payType < 1 || payType > 5) ? 1 : payType;
        // 支付方式 → 支付渠道标识（账目如实记录，避免“选微信却记现金”的对账错误）
        String channel;
        switch (payType) {
            case 2:
                channel = "WECHAT";
                break;
            case 3:
                channel = "ALIPAY";
                break;
            case 4:
                channel = "BANKCARD";
                break;
            case 5:
                channel = "MEMBER_BALANCE";
                break;
            default:
                channel = "CASH";
        }

        // 1. 幂等性检查：基于订单ID的CAS操作，防止网络重试/重复点击导致重复扣款
        if (!acquireCashPaymentLock(orderId)) {
            throw new IllegalArgumentException("收银失败：该订单正在处理中或已完成收银，请勿重复提交");
        }
        try {
            return doCashPayment(orderId, orderNumber, amount, actualAmount,
                    payType, cashierId, cashierName, usedCouponId, memberUserId, remark, channel);
        } finally {
            // 释放幂等锁：事务提交/回滚后均删除。TTL 3600s 仅作异常兜底（进程崩溃/GC 停顿
            // 时锁自然过期），正常路径由这里立即释放，避免同订单误报"处理中"。
            releaseCashPaymentLock(orderId);
        }
    }

    /**
     * 收银支付核心逻辑（在幂等锁保护下执行）。
     * 独立方法以便 finally 释放锁（@Transactional 只对 public 代理方法生效，
     * 若把释放放在本方法内，事务提交后锁已不在作用域，故由外层 cashPayment 统一释放）。
     */
    private CashierRecord doCashPayment(Long orderId, String orderNumber, BigDecimal amount, BigDecimal actualAmount,
                                        Integer payType, Long cashierId, String cashierName,
                                        Long usedCouponId, Long memberUserId, String remark, String channel) {

        // 幂等返回：已存在收银记录则直接返回（覆盖并发场景下先插记录后落锁的顺序差）
        // 修改点(2026-09-18)：幂等命中已收银记录时改为抛出明确业务异常。
        // 原实现直接 return existingRecord → 前端收到 R.success 提示「收款成功」，
        // 但实际未做任何事（桌台仍占用、可无限重复点击），属典型的「假成功」。
        CashierRecord existingRecord = cashierRecordMapper.selectOne(
                new LambdaQueryWrapper<CashierRecord>().eq(CashierRecord::getOrderId, orderId));
        if (existingRecord != null) {
            throw new CustomException("该订单已完成收银，请勿重复结账");
        }

        // 2. 加载订单并以服务端金额为准（防前端篡改应收金额）
        Orders order = loadAndValidateOrder(orderId, amount);
        BigDecimal orderAmount = order.getAmount();

        // 2. 计算优惠券抵扣（服务端按券规则重算，不信任前端传入的折后金额）
        BigDecimal couponDiscount = resolveCouponDiscount(usedCouponId, memberUserId, orderAmount);
        // 修改点(2026-09-18)：应用会员等级折扣——此前 MemberLevel.discount 在收银链路从未生效，
        // 会员页展示「黄金会员 8.5 折」但收银台按原价收款，前后端金额不一致。
        BigDecimal levelDiscount = resolveMemberLevelDiscount(memberUserId);
        // 应付金额 = (订单金额 - 券抵扣) × 等级折扣，四舍五入到分
        BigDecimal payable = orderAmount.subtract(couponDiscount).multiply(levelDiscount)
                .setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }

        // 3. 校验实收金额：非现金必须与应付一致；现金可多收（找零）
        validateActualAmount(payType, actualAmount, payable);

        // 4. 会员储值支付：真实扣减会员余额（余额不足则回滚）
        deductStoredBalanceIfNeeded(payType, memberUserId, payable);

        // 5. 计算找零（仅现金收银有找零）
        BigDecimal changeAmount = BigDecimal.ZERO;
        if (payType == 1 && actualAmount.compareTo(payable) > 0) {
            changeAmount = actualAmount.subtract(payable);
        }

        // 6-7. 创建并保存收银记录（金额以服务端计算为准）
        CashierRecord cashierRecord = buildCashierRecord(orderId, orderNumber, payType, orderAmount,
                actualAmount, changeAmount, cashierId, cashierName, remark);
        cashierRecordMapper.insert(cashierRecord);

        // 8. 更新订单状态为已支付（待接单），支付方式按真实选择记录；amount 回写为折后实收
        boolean eatIn = order.getTableId() != null
                && OrderSource.EAT_IN.getValue().equals(order.getSource());
        markOrderPaid(orderId, order, payType, usedCouponId, memberUserId, payable);

        // 9. 创建支付记录（金额按折后实收，退款/日结以此为据）
        saveSuccessPaymentOrder(orderId, channel, payable);

        // 10. 堂食单直接置「已完成(4)」，无后续自动流转，需在事务提交后补发完成事件，
        //     否则积分不发、usedCouponId 对应券不核销（可被重复使用）。afterCommit 保证
        //     异步监听器能读到已提交的实收金额与券ID。非堂食单流转到完成时由状态流服务发事件。
        if (eatIn) {
            publishCompletedAfterCommit(orderId, order.getTenantId());
        }

        return cashierRecord;
    }

    /**
     * 修改点(2026-09-18)：按桌台合并结账——一次性结清该桌台所有待付款堂食订单。
     * 解决「一桌多单时只收最后一单的钱」的资金缺口（扫码加菜每次新建独立订单，
     * 而 dining_table.current_order_id 只指向最后一单）。
     *
     * @param tableId      桌台ID
     * @param actualAmount 实收金额
     * @param payType      支付方式
     * @param cashierId    收银员ID
     * @param cashierName  收银员姓名
     * @param usedCouponId 使用的优惠券ID
     * @param memberUserId 会员用户ID
     * @param remark       备注
     * @return 收银记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashierRecord cashPaymentByTable(Long tableId, BigDecimal actualAmount, Integer payType,
                                            Long cashierId, String cashierName,
                                            Long usedCouponId, Long memberUserId, String remark) {
        if (tableId == null) {
            throw new CustomException("桌台ID不能为空");
        }
        // 1. 查询该桌台所有待付款堂食订单（租户隔离由拦截器 + 显式条件双重保证）
        List<Orders> orders = orderService.lambdaQuery()
                .eq(Orders::getTenantId, BaseContext.getCurrentTenantId())
                .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                .eq(Orders::getSource, OrderSource.EAT_IN.getValue())
                .eq(Orders::getTableId, tableId)
                .orderByAsc(Orders::getOrderTime)
                .list();
        if (orders == null || orders.isEmpty()) {
            throw new CustomException("该桌台没有待结账订单");
        }
        // 2. 幂等：任一张单已收银则整单拒绝，避免重复收款
        for (Orders o : orders) {
            CashierRecord exist = cashierRecordMapper.selectOne(
                    new LambdaQueryWrapper<CashierRecord>().eq(CashierRecord::getOrderId, o.getId()));
            if (exist != null) {
                throw new CustomException("该桌台存在已结账订单，请勿重复结账");
            }
        }
        // 3. 合并金额：逐单以服务端金额为准（占位单用订单明细汇总兜底），同时收集各单折前金额
        BigDecimal orderAmount = BigDecimal.ZERO;
        List<BigDecimal> detailAmounts = new ArrayList<>();
        for (Orders o : orders) {
            BigDecimal amt = o.getAmount();
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                amt = computeOrderDetailTotal(o.getId());
            }
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                amt = BigDecimal.ZERO;
            }
            detailAmounts.add(amt);
            orderAmount = orderAmount.add(amt);
        }
        if (orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("该桌台尚未点单，请先加菜后再结账");
        }
        // 4. 券抵扣 + 会员等级折扣（与普通收银同一口径）
        BigDecimal couponDiscount = resolveCouponDiscount(usedCouponId, memberUserId, orderAmount);
        BigDecimal levelDiscount = resolveMemberLevelDiscount(memberUserId);
        BigDecimal payable = orderAmount.subtract(couponDiscount).multiply(levelDiscount)
                .setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }
        // 合并实收按各单折前占比分摊，末单兜底差额，保证各单 amount 之和恰等于总实收
        List<BigDecimal> paidAmounts = allocatePaidAmounts(detailAmounts, payable);
        // 5. 实收校验 + 储值扣减
        validateActualAmount(payType, actualAmount, payable);
        deductStoredBalanceIfNeeded(payType, memberUserId, payable);

        // 6. 找零（仅现金）
        BigDecimal changeAmount = BigDecimal.ZERO;
        if (payType != null && payType == 1 && actualAmount.compareTo(payable) > 0) {
            changeAmount = actualAmount.subtract(payable);
        }
        // 7. 写一条收银记录（主单取最早一张，备注标注合并笔数）
        Orders main = orders.get(0);
        String mergedRemark = (remark == null ? "" : remark)
                + "【按桌台合并结账，共" + orders.size() + "笔订单】";
        CashierRecord record = buildCashierRecord(main.getId(), main.getNumber(), payType, orderAmount,
                actualAmount, changeAmount, cashierId, cashierName, mergedRemark);
        cashierRecordMapper.insert(record);

        // 8. 所有订单统一置「已完成(4)」，amount 回写为各单分摊实收，并写支付记录
        //    （堂食无 2→3→4 流转，直接完成）
        String channel = resolvePayChannel(payType);
        for (int idx = 0; idx < orders.size(); idx++) {
            Orders o = orders.get(idx);
            BigDecimal alloc = paidAmounts.get(idx);
            boolean updated = orderService.lambdaUpdate()
                    .eq(Orders::getId, o.getId())
                    .eq(Orders::getTenantId, o.getTenantId())
                    .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                    .set(Orders::getStatus, Orders.STATUS_COMPLETED)
                    .set(Orders::getPayMethod, payType)
                    .set(Orders::getCheckoutTime, LocalDateTime.now())
                    .set(Orders::getAmount, alloc)
                    .set(usedCouponId != null, Orders::getUsedCouponId, usedCouponId)
                    .set(memberUserId != null, Orders::getUserId, memberUserId)
                    .update();
            if (!updated) {
                throw new CustomException("订单状态已变更，请刷新后重试");
            }
            saveSuccessPaymentOrder(o.getId(), channel, alloc);
            // 堂食单直接完成，事务提交后补发完成事件：积分按实收发放、券核销
            publishCompletedAfterCommit(o.getId(), o.getTenantId());
        }
        // 9. 释放桌台（与订单更新同一事务，fail-closed 依赖租户上下文，不能异步）
        if (diningTableService != null) {
            try {
                diningTableService.changeStatus(tableId, DiningTableStatus.FREE.getValue());
                log.info("[收银] 桌台{}合并结账完成，已释放为空闲（共{}笔订单）", tableId, orders.size());
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免桌台释放失败回滚已完成的收款
                log.error("[收银] 桌台{}释放失败，需人工核查: {}", tableId, e.getMessage(), e);
            }
        }
        return record;
    }

    /**
     * 修改点(2026-09-18)：支付方式 → 支付渠道映射（与 saveSuccessPaymentOrder 的 channel 口径一致）。
     *
     * @param payType 支付方式
     * @return 渠道标识
     */
    private String resolvePayChannel(Integer payType) {
        if (payType == null) {
            return "CASH";
        }
        switch (payType) {
            case 2: return "WECHAT";
            case 3: return "ALIPAY";
            case 4: return "BANKCARD";
            case 5: return "MEMBER_BALANCE";
            default: return "CASH";
        }
    }

    /**
     * 修改点(2026-09-18)：结算预览——服务端权威计算应付金额，供收银台展示与提交使用。
     *
     * @param orderId      订单ID
     * @param usedCouponId 使用的优惠券ID
     * @param memberUserId 会员用户ID
     * @return 预览结果（orderAmount / couponDiscount / levelDiscount / payable）
     */
    @Override
    public Map<String, Object> previewCheckout(Long orderId, Long usedCouponId, Long memberUserId, Long tableId) {
        BigDecimal orderAmount;
        int orderCount = 1;
        boolean merged = false;
        Orders mainOrder;
        // 修改点(2026-09-18)：传入桌台ID时按「该桌台所有待付款堂食订单」合计预览，
        // 与按桌台合并结账保持同一口径（避免前端按单订单金额显示、后端按多单收款的落差）
        if (tableId != null) {
            List<Orders> orders = resolvePendingEatInOrders(tableId);
            if (orders == null || orders.isEmpty()) {
                throw new CustomException("该桌台没有待结账订单");
            }
            orderCount = orders.size();
            merged = orderCount > 1;
            mainOrder = orders.get(0);
            orderAmount = BigDecimal.ZERO;
            for (Orders o : orders) {
                BigDecimal amt = o.getAmount();
                if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                    amt = computeOrderDetailTotal(o.getId());
                }
                if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                    orderAmount = orderAmount.add(amt);
                }
            }
            if (orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException("该桌台尚未点单，请先加菜后再结账");
            }
        } else {
            mainOrder = loadOrderForPreview(orderId);
            orderAmount = mainOrder.getAmount();
        }
        BigDecimal couponDiscount = resolveCouponDiscount(usedCouponId, memberUserId, orderAmount);
        BigDecimal levelDiscount = resolveMemberLevelDiscount(memberUserId);
        BigDecimal payable = orderAmount.subtract(couponDiscount).multiply(levelDiscount)
                .setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", mainOrder != null ? mainOrder.getId() : orderId);
        data.put("tableId", tableId);
        data.put("orderAmount", orderAmount);
        data.put("couponDiscount", couponDiscount);
        data.put("levelDiscount", levelDiscount);
        data.put("payable", payable);
        data.put("orderCount", orderCount);
        data.put("merged", merged);
        return data;
    }

    /**
     * 修改点(2026-09-18)：查询指定桌台所有待付款堂食订单（按单时间升序，最早一张为主单）。
     *
     * @param tableId 桌台ID
     * @return 待付款堂食订单列表
     */
    private List<Orders> resolvePendingEatInOrders(Long tableId) {
        return orderService.lambdaQuery()
                .eq(Orders::getTenantId, BaseContext.getCurrentTenantId())
                .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                .eq(Orders::getSource, OrderSource.EAT_IN.getValue())
                .eq(Orders::getTableId, tableId)
                .orderByAsc(Orders::getOrderTime)
                .list();
    }

    /**
     * 修改点(2026-09-18)：预览场景加载订单（不做前端金额防篡改校验，占位单用明细汇总兜底）。
     *
     * @param orderId 订单ID
     * @return 订单
     */
    private Orders loadOrderForPreview(Long orderId) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在或已失效");
        }
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal detailSum = computeOrderDetailTotal(orderId);
            if (detailSum == null || detailSum.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException("该桌台尚未点单，请先加菜后再结账");
            }
            order.setAmount(detailSum);
        }
        return order;
    }

    /**
     * 加载订单并校验服务端金额（等价抽取，降低方法长度）。
     *
     * @param orderId 订单ID
     * @param amount 前端传入应收金额
     * @return 订单
     */
    private Orders loadAndValidateOrder(Long orderId, BigDecimal amount) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("收银失败：订单不存在或已失效");
        }
        BigDecimal originalDbAmount = order.getAmount();
        BigDecimal orderAmount = originalDbAmount;
        // 修改点(2026-09-16)：桌台/挂账占位订单 amount 初始为 0 且加菜/结账流程未回写，
        // 从订单明细实时汇总应收金额兜底，使此类订单可正常收银（根治"订单金额异常"）
        if (originalDbAmount == null || originalDbAmount.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal detailSum = computeOrderDetailTotal(orderId);
            if (detailSum != null && detailSum.compareTo(BigDecimal.ZERO) > 0) {
                orderAmount = detailSum;
                order.setAmount(detailSum);
                orderService.updateById(order); // 回写，保证营收统计/幂等一致
            } else {
                throw new IllegalArgumentException("收银失败：订单金额异常");
            }
        }
        // 防篡改：仅当 DB 原本金额有效时才校验前端传入金额；占位订单前端金额为 0，以服务端明细汇总为准
        if (originalDbAmount != null && originalDbAmount.compareTo(BigDecimal.ZERO) > 0
                && orderAmount.compareTo(amount) != 0) {
            throw new IllegalArgumentException("收银失败：订单金额与系统不一致，请刷新后重试");
        }
        return order;
    }

    /**
     * 修改点(2026-09-16)：从订单明细实时汇总应收金额（兜底桌台/挂账占位订单 amount 未回写的场景）。
     * order_detail.amount 为单项小计（= 单价 × 数量，见 OrderServiceImpl.buildOrderDetailsAndComputeAmount），直接求和即可。
     */
    private BigDecimal computeOrderDetailTotal(Long orderId) {
        List<OrderDetail> details = orderDetailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId).list();
        BigDecimal total = BigDecimal.ZERO;
        if (details != null) {
            for (OrderDetail d : details) {
                if (d.getAmount() != null) {
                    total = total.add(d.getAmount());
                }
            }
        }
        return total;
    }

    /**
     * 服务端重算优惠券抵扣金额（等价抽取，降低方法长度）。
     *
     * @param usedCouponId 使用的优惠券ID
     * @param memberUserId 会员用户ID
     * @param orderAmount 订单金额
     * @return 抵扣金额
     */
    private BigDecimal resolveCouponDiscount(Long usedCouponId, Long memberUserId, BigDecimal orderAmount) {
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (usedCouponId != null && memberUserId != null) {
            // 修改点(2026-09-18)：coupon_user.member_id 存的是「会员主键 member.id」，
            // 而收银台传入的是「user.id」，直接透传会导致券永远匹配不到 → 选了券但服务端不抵扣。
            Long memberId = resolveMemberId(memberUserId);
            List<CouponAvailableDTO> usable = couponUserService.availableCoupons(memberId, orderAmount);
            for (CouponAvailableDTO c : usable) {
                if (usedCouponId.equals(c.getId())) {
                    couponDiscount = c.getCurrentDiscount();
                    break;
                }
            }
        }
        return couponDiscount == null ? BigDecimal.ZERO : couponDiscount;
    }

    /**
     * 修改点(2026-09-18)：按 user.id 反查会员主键 member.id（券归属与等级折扣均按会员主键）。
     *
     * @param userId 用户ID
     * @return 会员主键；未找到或服务不可用时回退为原 userId
     */
    private Long resolveMemberId(Long userId) {
        if (userId == null || memberService == null) {
            return userId;
        }
        try {
            Member member = memberService.getByUserId(userId);
            return member != null ? member.getId() : userId;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，会员查询失败不应阻断结账
            log.warn("[收银] 会员信息查询失败，按原ID处理: userId={}", userId);
            return userId;
        }
    }

    /**
     * 修改点(2026-09-18)：计算会员等级折扣率（无会员/无等级/折扣非法时返回 1，即不打折）。
     *
     * @param memberUserId 会员用户ID
     * @return 折扣率（如 0.85 表示 8.5 折）
     */
    private BigDecimal resolveMemberLevelDiscount(Long memberUserId) {
        if (memberUserId == null || memberService == null || memberLevelService == null) {
            return BigDecimal.ONE;
        }
        try {
            Member member = memberService.getByUserId(memberUserId);
            if (member == null || member.getLevelId() == null) {
                return BigDecimal.ONE;
            }
            MemberLevel level = memberLevelService.getById(member.getLevelId());
            if (level == null || level.getDiscount() == null
                    || level.getDiscount().compareTo(BigDecimal.ZERO) <= 0
                    || level.getDiscount().compareTo(BigDecimal.ONE) > 0) {
                return BigDecimal.ONE;
            }
            return level.getDiscount();
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，折扣查询失败按不打折处理（宁可少收不可错收）
            log.warn("[收银] 会员等级折扣查询失败，按不打折处理: userId={}", memberUserId);
            return BigDecimal.ONE;
        }
    }

    /**
     * 校验实收金额（现金可多收，非现金须一致）（等价抽取）。
     *
     * @param payType 支付方式
     * @param actualAmount 实收金额
     * @param payable 应付金额
     */
    private void validateActualAmount(Integer payType, BigDecimal actualAmount, BigDecimal payable) {
        if (payType == 1) {
            if (actualAmount.compareTo(payable) < 0) {
                throw new IllegalArgumentException("收银失败：现金实收金额低于应付金额");
            }
        } else {
            // 修改点(2026-09-18)：允许 0.01 元容差——前端 JS 浮点减法
            //（例如 0.30 - 0.10 = 0.19999999999999998）与服务端 BigDecimal 精确值比对时，
            // 会偶发误报「实收金额与应付金额不一致」，导致用券后无法用非现金方式结账。
            BigDecimal diff = actualAmount.subtract(payable).abs();
            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                throw new IllegalArgumentException("收银失败：实收金额与应付金额不一致");
            }
        }
    }

    /**
     * 储值支付时扣减会员余额（等价抽取）。
     *
     * @param payType 支付方式
     * @param memberUserId 会员用户ID
     * @param payable 应付金额
     */
    private void deductStoredBalanceIfNeeded(Integer payType, Long memberUserId, BigDecimal payable) {
        if (payType != 5) {
            return;
        }
        if (memberUserId == null) {
            throw new IllegalArgumentException("收银失败：储值支付需先识别会员");
        }
        boolean deducted = memberRewardService.deductStoredBalance(memberUserId, payable);
        if (!deducted) {
            throw new IllegalArgumentException("收银失败：会员储值余额不足");
        }
    }

    /**
     * 构建收银记录（金额以服务端计算为准）（等价抽取）。
     *
     * @return 收银记录
     */
    private CashierRecord buildCashierRecord(Long orderId, String orderNumber, Integer payType, BigDecimal orderAmount,
                                             BigDecimal actualAmount, BigDecimal changeAmount, Long cashierId,
                                             String cashierName, String remark) {
        CashierRecord cashierRecord = new CashierRecord();
        cashierRecord.setOrderId(orderId);
        cashierRecord.setOrderNumber(orderNumber);
        cashierRecord.setPayType(payType);
        cashierRecord.setAmount(orderAmount);
        cashierRecord.setActualAmount(actualAmount);
        cashierRecord.setChangeAmount(changeAmount);
        cashierRecord.setCashierTime(LocalDateTime.now());
        cashierRecord.setCashierId(cashierId);
        cashierRecord.setCashierName(cashierName);
        cashierRecord.setRemark(remark);
        cashierRecord.setTenantId(BaseContext.getCurrentTenantId());
        cashierRecord.setCreateTime(LocalDateTime.now());
        cashierRecord.setCreateUser(cashierId);
        return cashierRecord;
    }

    /**
     * CAS 更新订单为已支付（待接单）（等价抽取）。
     */
    private void markOrderPaid(Long orderId, Orders order, Integer payType, Long usedCouponId,
                               Long memberUserId, BigDecimal paidAmount) {
        // 修改点(2026-09-18)：堂食订单收银完成即视为「已完成(4)」并在同一事务内释放桌台。
        // 原实现一律置为「待接单(2)」，而堂食订单没有 2→3→4 的自动流转路径，
        // 导致 OrderStatusFlowServiceImpl.releaseTableIfEatIn 永不触发——
        // 结账后桌台始终是占用态、可再次点结账，结账业务闭环断裂（本次修复的核心缺陷）。
        boolean eatIn = order.getTableId() != null
                && OrderSource.EAT_IN.getValue().equals(order.getSource());
        int targetStatus = eatIn ? Orders.STATUS_COMPLETED : Orders.STATUS_ORDERED;

        // CAS 状态机：仅 PENDING_PAY 状态的订单可收银，防止并发绕过状态机
        boolean updated = orderService.lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getTenantId, order.getTenantId())
                .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                .set(Orders::getStatus, targetStatus)
                .set(Orders::getPayMethod, payType)
                .set(Orders::getCheckoutTime, LocalDateTime.now())
                // 券/会员折扣后实收才是真实成交额：回写 amount，供积分、退款、营收报表统一口径
                .set(paidAmount != null, Orders::getAmount, paidAmount)
                .set(usedCouponId != null, Orders::getUsedCouponId, usedCouponId)
                .set(memberUserId != null, Orders::getUserId, memberUserId)
                .update();
        if (!updated) {
            throw new CustomException("订单状态已变更，请刷新后重试");
        }
        // 桌台释放必须在主事务内同步执行：DiningTableService.changeStatus 为 fail-closed
        // （依赖租户上下文），不能放到 afterCommit 异步回调里执行。
        if (eatIn) {
            releaseTableAfterCheckout(order);
        }
    }

    /**
     * 修改点(2026-09-18)：堂食结账完成后释放桌台为空闲（与订单取消/完成同策略）。
     * 释放失败仅记录日志告警，不回滚已完成的收款（资金优先，桌台状态由人工核对补修正）。
     */
    private void releaseTableAfterCheckout(Orders order) {
        if (diningTableService == null) {
            log.warn("[收银] 桌台服务未注入，跳过桌台释放: tableId={}", order.getTableId());
            return;
        }
        try {
            diningTableService.changeStatus(order.getTableId(), DiningTableStatus.FREE.getValue());
            log.info("[收银] 堂食结账完成，桌台{}已释放为空闲", order.getTableId());
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免桌台释放失败回滚已完成的收款
            log.error("[收银] 桌台{}释放失败，需人工核查: {}", order.getTableId(), e.getMessage(), e);
        }
    }

    /**
     * 创建成功支付记录（金额以服务端为准）（等价抽取）。
     */
    private void saveSuccessPaymentOrder(Long orderId, String channel, BigDecimal orderAmount) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderId(orderId);
        paymentOrder.setTenantId(BaseContext.getCurrentTenantId());
        paymentOrder.setTradeNo(channel + "_" + UUID.randomUUID().toString().replace("-", ""));
        paymentOrder.setChannel(channel);
        paymentOrder.setAmount(orderAmount);
        paymentOrder.setStatus(PaymentOrder.STATUS_SUCCESS);
        paymentOrder.setPaidTime(LocalDateTime.now());
        paymentOrder.setCreatedTime(LocalDateTime.now());
        paymentOrder.setUpdateTime(LocalDateTime.now());
        paymentOrderService.save(paymentOrder);
    }

    /**
     * 按各单折前金额占比分摊合并实收，末单兜底取差额，保证分摊之和恰等于总实收。
     *
     * @param detailAmounts 各单折前金额（与订单列表同序）
     * @param totalPaid     合并结账总实收
     * @return 各单分摊实收
     */
    private List<BigDecimal> allocatePaidAmounts(List<BigDecimal> detailAmounts, BigDecimal totalPaid) {
        int n = detailAmounts.size();
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal grossTotal = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            grossTotal = grossTotal.add(detailAmounts.get(i));
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                // 末单兜底，吸收前面四舍五入的累计误差
                result.add(totalPaid.subtract(allocated));
            } else {
                BigDecimal share = BigDecimal.ZERO;
                if (grossTotal.compareTo(BigDecimal.ZERO) > 0) {
                    share = detailAmounts.get(i).multiply(totalPaid)
                            .divide(grossTotal, 2, RoundingMode.HALF_UP);
                }
                result.add(share);
                allocated = allocated.add(share);
            }
        }
        return result;
    }

    /**
     * 在当前事务提交后发布订单完成事件；无事务上下文时直接发布（兜底）。
     * afterCommit 保证 @Async 权益监听器能读到已提交的实收金额与券ID。
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     */
    private void publishCompletedAfterCommit(final Long orderId, final Long tenantId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(
                            new OrderCompletedEvent(CashierServiceImpl.this, orderId, tenantId));
                }
            });
        } else {
            eventPublisher.publishEvent(new OrderCompletedEvent(this, orderId, tenantId));
        }
    }

    /**
     * 删除 cashier record。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCashierRecord(Long id) {
        return cashierRecordMapper.deleteById(id) > 0;
    }

    // ==================== 日结管理 ====================

    /**
     * 获取 daily settlement list。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<DailySettlement> getDailySettlementList(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(DailySettlement::getSettlementDate, startDate);
        }
        if (endDate != null) {
            qw.le(DailySettlement::getSettlementDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        qw.orderByDesc(DailySettlement::getSettlementDate);
        return dailySettlementMapper.selectList(qw);
    }

    /**
     * 获取 daily settlement by date。
     * @param settlementDate 参数 settlementDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public DailySettlement getDailySettlementByDate(LocalDate settlementDate, Long tenantId) {
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        qw.eq(DailySettlement::getSettlementDate, settlementDate);
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        return dailySettlementMapper.selectOne(qw);
    }

    /**
     * 处理 execute daily settlement。
     * @param settlementDate 参数 settlementDate
     * @param userId 参数 userId
     * @param userName 参数 userName
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailySettlement executeDailySettlement(LocalDate settlementDate, Long userId, String userName,
            Long tenantId) {
        // 按 tenantId+date 串行化日结请求，防止并发重复日结（TOCTOU）
        String lockKey = (tenantId != null ? tenantId.toString() : "0") + ":" + settlementDate.toString();
        Object lock = settlementLock.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            // 1. 检查是否已日结
            DailySettlement existing = getDailySettlementByDate(settlementDate, tenantId);
            if (existing != null && existing.getStatus() == 1) {
                throw new CustomException("该日期已日结，不能重复日结");
            }

            // 2. 查询当日订单（等价抽取）
            List<Orders> orders = queryDailyOrders(settlementDate, tenantId);

            // 3. 创建或复用日结记录
            DailySettlement settlement;
            if (existing != null) {
                settlement = existing;
            } else {
                settlement = new DailySettlement();
                settlement.setSettlementDate(settlementDate);
                settlement.setTenantId(tenantId);
            }

            // 4. 统计营业额与支付方式构成（等价抽取）
            applyDailyStats(settlement, orders);

            // 5. 计算净收入、成本、毛利润
            BigDecimal netIncome = settlement.getTotalRevenue().subtract(settlement.getRefundAmount());

            // 从 DishCost 表聚合当日已完成订单的材料/人工/其他成本
            // DishCost 按菜品维度记录单位成本（materialCost/laborCost/otherCost），
            // 乘以订单明细数量后汇总，得到当日总成本
            Map<String, BigDecimal> costBreakdown = aggregateDailyCosts(orders, tenantId);
            BigDecimal materialCost = costBreakdown.get("materialCost");
            BigDecimal laborCost = costBreakdown.get("laborCost");
            BigDecimal otherCost = costBreakdown.get("otherCost");
            BigDecimal totalCost = materialCost.add(laborCost).add(otherCost);

            BigDecimal grossProfit = netIncome.subtract(totalCost);
            BigDecimal profitRate = BigDecimal.ZERO;
            if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = grossProfit.divide(netIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }

            settlement.setNetIncome(netIncome);
            settlement.setMaterialCost(materialCost);
            settlement.setLaborCost(laborCost);
            settlement.setOtherCost(otherCost);
            settlement.setTotalCost(totalCost);
            settlement.setGrossProfit(grossProfit);
            settlement.setProfitRate(profitRate);
            settlement.setStatus(1); // 已结账
            settlement.setSettlementTime(LocalDateTime.now());
            settlement.setSettlementUserId(userId);
            settlement.setSettlementUserName(userName);

            // 6. 持久化（等价抽取）
            persistDailySettlement(settlement, existing == null, userId);
            return settlement;
        }
    }

    /**
     * 查询指定日期的订单（等价抽取，降低方法长度）。
     */
    private List<Orders> queryDailyOrders(LocalDate settlementDate, Long tenantId) {
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, settlementDate.atStartOfDay());
        orderQw.le(Orders::getOrderTime, settlementDate.atTime(LocalTime.MAX));
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        return orderService.list(orderQw);
    }

    /**
     * 统计营业额与支付方式构成并回填日结记录（等价抽取，降低方法长度）。
     */
    private void applyDailyStats(DailySettlement settlement, List<Orders> orders) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cashIncome = BigDecimal.ZERO;
        BigDecimal wechatIncome = BigDecimal.ZERO;
        BigDecimal alipayIncome = BigDecimal.ZERO;
        BigDecimal bankcardIncome = BigDecimal.ZERO;
        BigDecimal otherIncome = BigDecimal.ZERO;
        int orderCount = 0;
        BigDecimal refundAmount = BigDecimal.ZERO;
        int refundCount = 0;

        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                orderCount++;
                BigDecimal amount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(amount);

                // 按支付方式统计
                Integer payMethod = order.getPayMethod();
                if (payMethod == null) {
                    otherIncome = otherIncome.add(amount);
                } else if (payMethod == 1) {
                    cashIncome = cashIncome.add(amount);
                } else if (payMethod == 2) {
                    wechatIncome = wechatIncome.add(amount);
                } else if (payMethod == 3) {
                    alipayIncome = alipayIncome.add(amount);
                } else if (payMethod == 4) {
                    bankcardIncome = bankcardIncome.add(amount);
                } else {
                    otherIncome = otherIncome.add(amount);
                }
            } else if (order.getStatus() == Orders.STATUS_REFUNDED) {
                refundCount++;
                refundAmount = refundAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            }
        }

        settlement.setTotalRevenue(totalRevenue);
        settlement.setCashIncome(cashIncome);
        settlement.setWechatIncome(wechatIncome);
        settlement.setAlipayIncome(alipayIncome);
        settlement.setBankcardIncome(bankcardIncome);
        settlement.setOtherIncome(otherIncome);
        settlement.setOrderCount(orderCount);
        settlement.setRefundAmount(refundAmount);
        settlement.setRefundCount(refundCount);
    }

    /**
     * 聚合当日已完成订单的菜品成本（材料/人工/其他）。
     * 遍历已完成订单的明细，按 dishId 查 DishCost 表获取单位成本，乘以数量后汇总。
     * 未配置成本的菜品按 0 处理（不阻断日结流程）。
     */
    private Map<String, BigDecimal> aggregateDailyCosts(List<Orders> orders, Long tenantId) {
        BigDecimal materialCost = BigDecimal.ZERO;
        BigDecimal laborCost = BigDecimal.ZERO;
        BigDecimal otherCost = BigDecimal.ZERO;

        // 收集已完成订单的 ID
        List<Long> completedOrderIds = new ArrayList<>();
        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                completedOrderIds.add(order.getId());
            }
        }

        if (!completedOrderIds.isEmpty()) {
            // 批量查订单明细（按 orderId IN 查询）
            for (Long orderId : completedOrderIds) {
                List<OrderDetail> details = orderDetailService.listByOrderId(orderId);
                for (OrderDetail detail : details) {
                    if (detail.getDishId() == null) { continue; }
                    int qty = detail.getNumber() != null ? detail.getNumber() : 1;
                    // 查该菜品的单位成本
                    DishCost dc = costService.getDishCostByDishId(detail.getDishId(), tenantId);
                    if (dc == null) { continue; }
                    BigDecimal mc = dc.getMaterialCost() != null ? dc.getMaterialCost() : BigDecimal.ZERO;
                    BigDecimal lc = dc.getLaborCost() != null ? dc.getLaborCost() : BigDecimal.ZERO;
                    BigDecimal oc = dc.getOtherCost() != null ? dc.getOtherCost() : BigDecimal.ZERO;
                    materialCost = materialCost.add(mc.multiply(new BigDecimal(qty)));
                    laborCost = laborCost.add(lc.multiply(new BigDecimal(qty)));
                    otherCost = otherCost.add(oc.multiply(new BigDecimal(qty)));
                }
            }
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("materialCost", materialCost);
        result.put("laborCost", laborCost);
        result.put("otherCost", otherCost);
        return result;
    }

    /**
     * 持久化日结记录：新增或更新（等价抽取）。
     */
    private void persistDailySettlement(DailySettlement settlement, boolean isNew, Long userId) {
        if (isNew) {
            settlement.setCreateTime(LocalDateTime.now());
            settlement.setCreateUser(userId);
            dailySettlementMapper.insert(settlement);
        } else {
            settlement.setUpdateTime(LocalDateTime.now());
            settlement.setUpdateUser(userId);
            dailySettlementMapper.updateById(settlement);
        }
    }

    /**
     * 取消 daily settlement。
     * @param settlementDate 参数 settlementDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelDailySettlement(LocalDate settlementDate, Long tenantId) {
        DailySettlement settlement = getDailySettlementByDate(settlementDate, tenantId);
        if (settlement == null) {
            throw new CustomException("日结记录不存在");
        }
        if (settlement.getStatus() == 0) {
            throw new CustomException("日结已取消，不能重复取消");
        }

        settlement.setStatus(0); // 未结账
        settlement.setUpdateTime(LocalDateTime.now());
        return dailySettlementMapper.updateById(settlement) > 0;
    }

    /**
     * 删除 daily settlement。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDailySettlement(Long id) {
        return dailySettlementMapper.deleteById(id) > 0;
    }

    // ==================== 统计分析 ====================

    /**
     * 获取 cashier statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getCashierStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询收银记录
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.ge(CashierRecord::getCashierTime, startDate);
        qw.le(CashierRecord::getCashierTime, endDate);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        List<CashierRecord> records = cashierRecordMapper.selectList(qw);

        // 统计
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;
        for (CashierRecord record : records) {
            totalAmount = totalAmount.add(record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO);
            totalCount++;
        }

        result.put("totalAmount", totalAmount);
        result.put("totalCount", totalCount);
        result.put("avgAmount", totalCount > 0 ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode
                .HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    /**
     * 获取 payment type statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getPaymentTypeStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询收银记录
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.ge(CashierRecord::getCashierTime, startDate);
        qw.le(CashierRecord::getCashierTime, endDate);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        List<CashierRecord> records = cashierRecordMapper.selectList(qw);

        // 按支付方式统计
        Map<Integer, BigDecimal> typeAmountMap = new HashMap<>();
        Map<Integer, Integer> typeCountMap = new HashMap<>();

        for (CashierRecord record : records) {
            Integer payType = record.getPayType();
            BigDecimal amount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;

            typeAmountMap.merge(payType, amount, BigDecimal::add);
            typeCountMap.merge(payType, 1, Integer::sum);
        }

        // 构建结果
        List<Map<String, Object>> typeList = new ArrayList<>();
        String[] typeNames = {"现金", "微信", "支付宝", "银行卡", "其他"};

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", i);
            item.put("typeName", typeNames[i - 1]);
            item.put("amount", typeAmountMap.getOrDefault(i, BigDecimal.ZERO));
            item.put("count", typeCountMap.getOrDefault(i, 0));
            typeList.add(item);
        }

        result.put("typeList", typeList);
        return result;
    }

    /**
     * 获取 cashier trend。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getCashierTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 按天统计
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            dates.add(current.toString());

            // 查询当天收银记录
            LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
            qw.ge(CashierRecord::getCashierTime, current.atStartOfDay());
            qw.le(CashierRecord::getCashierTime, current.atTime(LocalTime.MAX));
            if (tenantId != null) {
                qw.eq(CashierRecord::getTenantId, tenantId);
            }
            List<CashierRecord> records = cashierRecordMapper.selectList(qw);

            BigDecimal dayAmount = BigDecimal.ZERO;
            int dayCount = 0;
            for (CashierRecord record : records) {
                dayAmount = dayAmount.add(record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO);
                dayCount++;
            }

            amounts.add(dayAmount);
            counts.add(dayCount);

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("amounts", amounts);
        result.put("counts", counts);

        return result;
    }

    /**
     * 获取 daily settlement summary。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getDailySettlementSummary(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询日结记录
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        qw.ge(DailySettlement::getSettlementDate, startDate);
        qw.le(DailySettlement::getSettlementDate, endDate);
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        List<DailySettlement> settlements = dailySettlementMapper.selectList(qw);

        // 统计
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalNetIncome = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;
        int totalOrders = 0;

        for (DailySettlement settlement : settlements) {
            totalRevenue = totalRevenue.add(settlement.getTotalRevenue() != null ? settlement
                    .getTotalRevenue() : BigDecimal.ZERO);
            totalRefund = totalRefund.add(settlement.getRefundAmount() != null ? settlement
                    .getRefundAmount() : BigDecimal.ZERO);
            totalNetIncome = totalNetIncome.add(settlement.getNetIncome() != null ? settlement
                    .getNetIncome() : BigDecimal.ZERO);
            totalCost = totalCost.add(settlement.getTotalCost() != null ? settlement.getTotalCost() : BigDecimal.ZERO);
            totalGrossProfit = totalGrossProfit.add(settlement.getGrossProfit() != null ? settlement
                    .getGrossProfit() : BigDecimal.ZERO);
            totalOrders += settlement.getOrderCount() != null ? settlement.getOrderCount() : 0;
        }

        // 计算平均毛利率
        BigDecimal avgProfitRate = BigDecimal.ZERO;
        if (totalNetIncome.compareTo(BigDecimal.ZERO) > 0) {
            avgProfitRate = totalGrossProfit.divide(totalNetIncome, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        result.put("totalRevenue", totalRevenue);
        result.put("totalRefund", totalRefund);
        result.put("totalNetIncome", totalNetIncome);
        result.put("totalCost", totalCost);
        result.put("totalGrossProfit", totalGrossProfit);
        result.put("avgProfitRate", avgProfitRate);
        result.put("totalOrders", totalOrders);
        result.put("settlementCount", settlements.size());

        return result;
    }

    // ==================== 幂等工具 ====================

    /**
     * 获取收银支付幂等锁（CAS）
     * <p>
     * 修复说明：原 cashPayment 方法无幂等检查，网络重试/用户重复点击可导致同一订单
     * 重复扣款、重复创建收银记录。此处使用 Redis SETNX 实现基于订单ID的CAS幂等锁，
     * 锁内先查再写，保证同一订单在同一租户下只处理一次。
     * <p>
     * Redis 不可用时自动降级为跳过幂等检查（保守降级为不过度阻塞业务），
     * 此时依赖数据库 cashier_record 表的 orderId 唯一约束兜底（建议在 cashier_record
     * 表添加 orderId 唯一索引以形成 DB 层第二道防线）。
     *
     * @param orderId 订单ID
     * @return true=获取成功，false=已存在锁（其他请求正在处理）
     */
    private boolean acquireCashPaymentLock(Long orderId) {
        if (stringRedisTemplate == null || orderId == null) {
            // Redis 不可用或订单ID缺失时跳过幂等检查，依赖 DB 层兜底
            return true;
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        String key = CASHIER_IDEMPOTENCY_KEY_PREFIX + (tenantId != null ? tenantId : 0) + ":" + orderId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, UUID.randomUUID().toString(), CASHIER_IDEMPOTENCY_TTL_SECONDS, TimeUnit.SECONDS);
        return acquired != null && acquired;
    }

    /**
     * 释放收银支付幂等锁。
     * <p>
     * 与 acquireCashPaymentLock 成对使用，正常路径在事务提交/回滚后立即释放，
     * 避免同订单误报"处理中"；删除失败不阻断业务（TTL 3600s 兜底自动过期）。
     * 注：锁持有窗口极短（毫秒级事务），简单 delete 即可；极端情况下锁因
     * 超时被接管后再被旧请求 delete，最坏效果是提前释放新锁——由 DB 层
     * cashier_record.order_id 唯一索引兜底（真重复扣款会被唯一约束拦截），
     * 不会造成重复入账。
     */
    private void releaseCashPaymentLock(Long orderId) {
        if (stringRedisTemplate == null || orderId == null) {
            return;
        }
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            String key = CASHIER_IDEMPOTENCY_KEY_PREFIX + (tenantId != null ? tenantId : 0) + ":" + orderId;
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("释放收银幂等锁失败（TTL兜底自动过期）：orderId={}", orderId);
        }
    }
}






