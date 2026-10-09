package com.reggie.module.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.finance.mapper.WithdrawalApplicationMapper;
import com.reggie.module.finance.mapper.ReconciliationStatementMapper;
import com.reggie.module.finance.mapper.ProfitAnalysisMapper;
import com.reggie.module.finance.model.WithdrawalApplication;
import com.reggie.module.finance.model.ReconciliationStatement;
import com.reggie.module.finance.model.ProfitAnalysis;
import com.reggie.module.finance.service.FinanceService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.cost.service.CostService;
import com.reggie.module.order.model.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Finance Service Implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@Service
public class FinanceServiceImpl extends ServiceImpl<WithdrawalApplicationMapper, WithdrawalApplication> implements
        FinanceService {

    @Autowired
    private WithdrawalApplicationMapper withdrawalMapper;

    @Autowired
    private ReconciliationStatementMapper reconciliationMapper;

    @Autowired
    private ProfitAnalysisMapper profitAnalysisMapper;

    /**
     * 对账生成防重锁 key 前缀（后接 tenantId:platform:date）
     */
    private static final String RECONCILIATION_LOCK_PREFIX = "finance:reconciliation:generate:";

    /**
     * 利润分析生成防重锁 key 前缀（后接 tenantId:date）
     */
    private static final String PROFIT_LOCK_PREFIX = "finance:profit:generate:";

    /**
     * 生成任务分布式锁 TTL（秒）：对账/利润生成需扫描全量订单并聚合成本，按业务耗时上限给足 10 分钟；
     * 进程崩溃或请求被中断时锁由 Redis 到期自动释放，不会把该周期永久锁死。
     */
    private static final long GENERATE_LOCK_TTL_SECONDS = 10 * 60L;

    /**
     * acquireGenerateLock 返回值哨兵：Redis 不可用（Bean 缺失或连接异常），与 "锁被他人占用" 区分——
     * 占用必须拒绝，不可用则降级为仅靠落库前查重兜底，不因基础设施抖动阻断功能。
     */
    private static final String LOCK_REDIS_UNAVAILABLE = "REDIS_UNAVAILABLE";

    /**
     * 分布式锁释放脚本：仅当锁值等于本次持有值时才删除，防止 TTL 到期后误删他人持有的锁。
     */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 集群防重锁依赖：可选注入，缺失时生成任务降级为"仅落库前查重"（见 {@link #acquireGenerateLock(String)}）。
     */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CostService costService;

    // ==================== Withdrawal Management ====================

    /**
     * 获取 withdrawal list。
     * @param status 参数 status
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<WithdrawalApplication> getWithdrawalList(Integer status, LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId) {
        LambdaQueryWrapper<WithdrawalApplication> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(WithdrawalApplication::getStatus, status);
        }
        if (startDate != null) {
            qw.ge(WithdrawalApplication::getCreateTime, startDate);
        }
        if (endDate != null) {
            qw.le(WithdrawalApplication::getCreateTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        qw.orderByDesc(WithdrawalApplication::getCreateTime);
        return withdrawalMapper.selectList(qw);
    }

    /**
     * 获取 withdrawal by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public WithdrawalApplication getWithdrawalById(Long id) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return null;
        }
        // 租户隔离校验：防止跨租户读取提现单敏感信息（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法查看提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权查看其他租户的提现单");
        }
        return application;
    }

    /**
     * 创建 withdrawal。
     * @param application 参数 application
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createWithdrawal(WithdrawalApplication application) {
        application.setApplicationNo(generateApplicationNo());
        application.setStatus(WithdrawalApplication.STATUS_PENDING);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        return withdrawalMapper.insert(application) > 0;
    }

    /**
     * 审核 withdrawal。
     * @param id 参数 id
     * @param status 参数 status
     * @param reviewerId 参数 reviewerId
     * @param reviewerName 参数 reviewerName
     * @param remark 参数 remark
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewWithdrawal(Long id, Integer status, Long reviewerId, String reviewerName, String remark) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }
        // 租户隔离校验：禁止跨租户审批（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法审批提现申请");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权审批其他租户的提现申请");
        }
        // 状态机校验：仅待审批状态可审批
        if (application.getStatus() != WithdrawalApplication.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待审批状态的提现申请可审批");
        }
        // 审批结果校验：仅允许审批通过或拒绝
        if (status != WithdrawalApplication.STATUS_APPROVED && status != WithdrawalApplication.STATUS_REJECTED) {
            throw new IllegalArgumentException("无效的审批结果");
        }

        // 并发防护（P1）：先 CAS 抢占 PENDING -> 目标状态，仅 affected rows=1 的事务可完成审批。
        // 此前 SELECT 校验 + updateById 无 CAS：并发双审批可同时通过 PENDING 校验并各自覆盖更新，
        // 且 approve 后资金扣减/出账无状态互斥。现条件更新互斥，第二个请求 rows=0 返回失败。
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<WithdrawalApplication>()
                .eq(WithdrawalApplication::getId, id)
                .eq(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_PENDING)
                .set(WithdrawalApplication::getStatus, status)
                .set(WithdrawalApplication::getReviewerId, reviewerId)
                .set(WithdrawalApplication::getReviewerName, reviewerName)
                .set(WithdrawalApplication::getReviewTime, LocalDateTime.now())
                .set(WithdrawalApplication::getReviewRemark, remark)
                .set(WithdrawalApplication::getUpdateTime, LocalDateTime.now()));
        if (claimed == 0) {
            throw new IllegalArgumentException("提现申请状态已变更，请勿重复审批");
        }
        return true;
    }

    /**
     * 处理 withdrawal payment。
     * @param id 参数 id
     * @param paymentNo 参数 paymentNo
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processWithdrawalPayment(Long id, String paymentNo) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }
        // 租户隔离校验：禁止跨租户付款（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法操作提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权操作其他租户的提现单");
        }
        // 状态机校验：仅已审批状态可付款
        if (application.getStatus() != WithdrawalApplication.STATUS_APPROVED) {
            throw new IllegalArgumentException("仅已审批状态的提现申请可付款");
        }

        // 并发防护（P1）：先 CAS 抢占 APPROVED -> PAID，仅 affected rows=1 的事务可完成付款。
        // 此前 SELECT 校验 + updateById 无 CAS：并发双付款（或付款+取消竞态）可同时通过校验并各自
        // 覆盖更新，导致同一提现单被重复出账。现条件更新互斥，第二个请求 rows=0 直接拒绝。
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<WithdrawalApplication>()
                .eq(WithdrawalApplication::getId, id)
                .eq(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_APPROVED)
                .set(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_PAID)
                .set(WithdrawalApplication::getPaymentTime, LocalDateTime.now())
                .set(WithdrawalApplication::getPaymentNo, paymentNo)
                .set(WithdrawalApplication::getUpdateTime, LocalDateTime.now()));
        if (claimed == 0) {
            throw new IllegalArgumentException("提现申请状态已变更，请勿重复付款");
        }
        return true;
    }

    /**
     * 取消 withdrawal。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWithdrawal(Long id) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }
        // 租户隔离校验：禁止跨租户取消（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法操作提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权操作其他租户的提现单");
        }
        if (application.getStatus() != WithdrawalApplication.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待审批状态的提现申请可取消");
        }

        // CAS 并发防护：仅 PENDING 状态可取消，防止并发取消+审批竞态
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<WithdrawalApplication>()
                .eq(WithdrawalApplication::getId, id)
                .eq(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_PENDING)
                .set(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_CANCELLED)
                .set(WithdrawalApplication::getUpdateTime, LocalDateTime.now()));
        if (claimed == 0) {
            throw new IllegalArgumentException("提现申请状态已变更，无法取消");
        }
        return true;
    }

    /**
     * 删除 withdrawal。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithdrawal(Long id) {
        return withdrawalMapper.deleteById(id) > 0;
    }

    // ==================== Reconciliation Management ====================

    /**
     * 获取 reconciliation list。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param platform 参数 platform
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<ReconciliationStatement> getReconciliationList(LocalDate startDate, LocalDate endDate, String platform,
            Long tenantId) {
        LambdaQueryWrapper<ReconciliationStatement> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(ReconciliationStatement::getStatementDate, startDate);
        }
        if (endDate != null) {
            qw.le(ReconciliationStatement::getStatementDate, endDate);
        }
        if (platform != null && !platform.isEmpty()) {
            qw.eq(ReconciliationStatement::getPlatform, platform);
        }
        if (tenantId != null) {
            qw.eq(ReconciliationStatement::getTenantId, tenantId);
        }
        qw.orderByDesc(ReconciliationStatement::getStatementDate);
        return reconciliationMapper.selectList(qw);
    }

    /**
     * 获取 reconciliation by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public ReconciliationStatement getReconciliationById(Long id) {
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return null;
        }
        // 租户归属校验：租户缺失时 fail-closed（禁止跨租户越权查看对账单）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法查看对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权查看其他租户的对账单");
        }
        return statement;
    }

    /**
     * 生成 reconciliation。
     * @param date 参数 date
     * @param platform 参数 platform
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public ReconciliationStatement generateReconciliation(LocalDate date, String platform, Long tenantId) {
        // 集群防重（P0）：Redis SETNX 锁按 tenantId+platform+date 串行化生成请求。
        // 原先用 JVM 内 ConcurrentHashMap.computeIfAbsent + synchronized 锁对象，多实例部署下各实例各自加锁
        //＝形同虚设，且锁 Map 的 key 永不清理（只增不减）造成内存泄漏。
        // 本方法刻意不加 @Transactional：方法体只有一条 insert，autocommit 保证数据在释放锁之前已对其他实例
        // 可见；若被事务包裹，锁会在事务提交前释放，第二个实例拿到锁后读不到未提交的行，重复生成竞态依旧存在。
        String lockKey = RECONCILIATION_LOCK_PREFIX + lockTenantPart(tenantId) + ":" + platform + ":" + date;
        String lockValue = acquireGenerateLock(lockKey);
        try {
            // 双保险：落库前按 (tenant_id, 平台, 对账日期) 条件查重，已存在直接返回，不重复生成
            LambdaQueryWrapper<ReconciliationStatement> qw = new LambdaQueryWrapper<>();
            qw.eq(ReconciliationStatement::getStatementDate, date);
            qw.eq(ReconciliationStatement::getPlatform, platform);
            if (tenantId != null) {
                qw.eq(ReconciliationStatement::getTenantId, tenantId);
            }
            ReconciliationStatement existing = reconciliationMapper.selectOne(qw);
            if (existing != null) {
                return existing;
            }

            // Query orders for the date
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.ge(Orders::getOrderTime, date.atStartOfDay());
            orderQw.le(Orders::getOrderTime, date.atTime(LocalTime.MAX));
            if (tenantId != null) {
                orderQw.eq(Orders::getTenantId, tenantId);
            }
            List<Orders> orders = orderService.list(orderQw);

            BigDecimal systemAmount = BigDecimal.ZERO;
            int orderCount = 0;
            BigDecimal refundAmount = BigDecimal.ZERO;
            int refundCount = 0;

            for (Orders order : orders) {
                if (order.getStatus() == Orders.STATUS_COMPLETED) {
                    orderCount++;
                    systemAmount = systemAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                } else if (order.getStatus() == Orders.STATUS_REFUNDED) {
                    refundCount++;
                    refundAmount = refundAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                }
            }

            // Create reconciliation statement
            ReconciliationStatement statement = new ReconciliationStatement();
            statement.setStatementNo("RC" + date.toString().replace("-", "") + System.currentTimeMillis() % 10000);
            statement.setStatementDate(date);
            statement.setPlatform(platform);
            statement.setSystemAmount(systemAmount);
            statement.setPlatformAmount(BigDecimal.ZERO); // To be filled manually
            statement.setDifferenceAmount(BigDecimal.ZERO);
            statement.setOrderCount(orderCount);
            statement.setRefundAmount(refundAmount);
            statement.setRefundCount(refundCount);
            statement.setFeeAmount(BigDecimal.ZERO);
            statement.setNetAmount(systemAmount.subtract(refundAmount));
            statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
            statement.setTenantId(tenantId);
            statement.setCreateTime(LocalDateTime.now());
            statement.setUpdateTime(LocalDateTime.now());

            reconciliationMapper.insert(statement);
            return statement;
        } finally {
            releaseGenerateLock(lockKey, lockValue);
        }
    }

    /**
     * 确认 reconciliation。
     * @param id 参数 id
     * @param userId 参数 userId
     * @param userName 参数 userName
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReconciliation(Long id, Long userId, String userName) {
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return false;
        }
        // 租户归属校验：租户缺失时 fail-closed（不允许确认他人对账单）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法操作对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权确认其他租户的对账单");
        }

        // Calculate difference
        // 防御性 null 检查：systemAmount/platformAmount 可能在数据库中为 null（历史数据）
        BigDecimal systemAmount = statement.getSystemAmount() != null ? statement.getSystemAmount() : BigDecimal.ZERO;
        BigDecimal platformAmount = statement.getPlatformAmount() != null ? statement.getPlatformAmount() : BigDecimal
                .ZERO;
        BigDecimal difference = systemAmount.subtract(platformAmount);
        statement.setDifferenceAmount(difference);
        statement.setStatus(difference.abs().compareTo(new BigDecimal("0.01")) < 0 ?
                ReconciliationStatement.STATUS_RECONCILED : ReconciliationStatement.STATUS_DISCREPANCY);
        statement.setReconcileTime(LocalDateTime.now());
        statement.setReconcileUserId(userId);
        statement.setReconcileUserName(userName);
        statement.setUpdateTime(LocalDateTime.now());

        return reconciliationMapper.updateById(statement) > 0;
    }

    /**
     * 删除 reconciliation。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReconciliation(Long id) {
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return false;
        }
        // 租户归属校验：租户缺失时 fail-closed
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法操作对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权删除其他租户的对账单");
        }
        return reconciliationMapper.deleteById(id) > 0;
    }

    // ==================== Profit Analysis ====================

    /**
     * 获取 profit analysis list。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<ProfitAnalysis> getProfitAnalysisList(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LambdaQueryWrapper<ProfitAnalysis> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(ProfitAnalysis::getAnalysisDate, startDate);
        }
        if (endDate != null) {
            qw.le(ProfitAnalysis::getAnalysisDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(ProfitAnalysis::getTenantId, tenantId);
        }
        qw.orderByDesc(ProfitAnalysis::getAnalysisDate);
        return profitAnalysisMapper.selectList(qw);
    }

    /**
     * 获取 profit analysis by date。
     * @param date 参数 date
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public ProfitAnalysis getProfitAnalysisByDate(LocalDate date, Long tenantId) {
        LambdaQueryWrapper<ProfitAnalysis> qw = new LambdaQueryWrapper<>();
        qw.eq(ProfitAnalysis::getAnalysisDate, date);
        if (tenantId != null) {
            qw.eq(ProfitAnalysis::getTenantId, tenantId);
        }
        return profitAnalysisMapper.selectOne(qw);
    }

    /**
     * 生成 profit analysis。
     * @param date 参数 date
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public ProfitAnalysis generateProfitAnalysis(LocalDate date, Long tenantId) {
        // 集群防重（P0）：Redis SETNX 锁按 tenantId+date 串行化利润生成请求，替换原 JVM 内
        // ConcurrentHashMap 锁（多实例失效 + 锁 Map 只增不减的内存泄漏）。
        // 刻意不加 @Transactional（同 generateReconciliation）：方法体只有一条 insert，autocommit
        // 保证落库先于释放锁对其它实例可见。
        String lockKey = PROFIT_LOCK_PREFIX + lockTenantPart(tenantId) + ":" + date;
        String lockValue = acquireGenerateLock(lockKey);
        try {
            // 双保险：落库前按 (tenant_id, 分析日期) 条件查重，已存在直接返回，不重复生成
            ProfitAnalysis existing = getProfitAnalysisByDate(date, tenantId);
            if (existing != null) {
                return existing;
            }

            // Query orders
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.ge(Orders::getOrderTime, date.atStartOfDay());
            orderQw.le(Orders::getOrderTime, date.atTime(LocalTime.MAX));
            if (tenantId != null) {
                orderQw.eq(Orders::getTenantId, tenantId);
            }
            List<Orders> orders = orderService.list(orderQw);

            // 收入统计（等价抽取）
            Map<String, Object> stats = computeRevenueStats(orders);
            BigDecimal totalRevenue = (BigDecimal) stats.get("totalRevenue");
            int orderCount = (Integer) stats.get("orderCount");
            int customerCount = (Integer) stats.get("customerCount");

            // Query costs
            Map<String, Object> costSummary = costService.getCostSummary(date, date, tenantId);
            // 计算利润并保存（等价抽取）
            return buildAndSaveProfitAnalysis(date, tenantId, totalRevenue, orderCount, customerCount, costSummary);
        } finally {
            releaseGenerateLock(lockKey, lockValue);
        }
    }

    // ==================== 生成任务分布式锁 ====================

    /**
     * 锁 key 的租户段：入参 tenantId 为空时回退当前登录租户，仍为空则用 "0"（与历史 key 组成一致）。
     */
    private String lockTenantPart(Long tenantId) {
        if (tenantId != null) {
            return tenantId.toString();
        }
        Long current = BaseContext.getCurrentTenantId();
        return current != null ? current.toString() : "0";
    }

    /**
     * 获取生成任务分布式锁：SETNX + TTL，锁值为 UUID 以便释放时做 ownership 校验。
     * <p>降级口径：锁被他人占用 → fail-closed 拒绝（重复生成会产出重复报表且全量扫描订单）；
     * Redis 不可用（Bean 缺失或连接异常）→ fail-open 返回哨兵，仅由"落库前查重"兜底，
     * 与 payment 模块（RefundServiceImpl / PaymentController）保持一致。</p>
     *
     * @param lockKey 锁 key
     * @return 锁值（UUID），或 {@link #LOCK_REDIS_UNAVAILABLE}（Redis 不可用，调用方无需释放）
     * @throws CustomException 锁被占用："该周期任务正在执行中"
     */
    private String acquireGenerateLock(String lockKey) {
        if (stringRedisTemplate == null) {
            log.warn("[周期生成] StringRedisTemplate 不可用，降级为仅落库前查重: {}", lockKey);
            return LOCK_REDIS_UNAVAILABLE;
        }
        String lockValue = UUID.randomUUID().toString();
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, GENERATE_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                return lockValue;
            }
            throw new CustomException("该周期任务正在执行中");
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            // 宽异常兜底：Redis 故障不外抛堆栈，降级为无锁 + 落库前查重
            log.error("[周期生成] 获取分布式锁异常，降级为仅落库前查重: {}, error={}", lockKey, e.getMessage(), e);
            return LOCK_REDIS_UNAVAILABLE;
        }
    }

    /**
     * 释放生成任务分布式锁：Lua 脚本比对锁值后删除，避免误删 TTL 过期后他人重新持有的锁。
     * 降级路径（哨兵值）不执行任何删除。
     */
    private void releaseGenerateLock(String lockKey, String lockValue) {
        if (stringRedisTemplate == null || lockValue == null || LOCK_REDIS_UNAVAILABLE.equals(lockValue)) {
            return;
        }
        try {
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(UNLOCK_LUA, Long.class),
                    Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            // 释放失败不影响业务结果，锁最迟在 TTL 到期后自动释放
            log.warn("[周期生成] 释放分布式锁失败，将由 TTL 兜底过期: {}, error={}", lockKey, e.getMessage(), e);
        }
    }

    /**
     * 统计已完成订单的收入、订单数与去重客户数（等价抽取，降低方法长度）。
     *
     * @return {totalRevenue, orderCount, customerCount}
     */
    private Map<String, Object> computeRevenueStats(List<Orders> orders) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int orderCount = 0;
        Set<Long> uniqueCustomers = new HashSet<>();
        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                orderCount++;
                totalRevenue = totalRevenue.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                if (order.getUserId() != null) {
                    uniqueCustomers.add(order.getUserId());
                }
            }
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("orderCount", orderCount);
        stats.put("customerCount", uniqueCustomers.size());
        return stats;
    }

    /**
     * 根据收入与成本计算利润指标并保存利润分析（等价抽取，降低方法长度）。
     */
    private ProfitAnalysis buildAndSaveProfitAnalysis(LocalDate date, Long tenantId, BigDecimal totalRevenue,
            int orderCount, int customerCount, Map<String, Object> costSummary) {
        // 类型安全取值：getOrDefault 若命中 key 但 value 非 BigDecimal（如 Integer/Long），
        // 直接强转 ClassCastException。此处走安全转换路径，并对 null 兜底。
        BigDecimal foodCost = toBigDecimal(costSummary.get("materialCost"));
        BigDecimal laborCost = toBigDecimal(costSummary.get("laborCost"));
        BigDecimal otherCost = toBigDecimal(costSummary.get("otherCost"));
        BigDecimal totalCost = foodCost.add(laborCost).add(otherCost);

        // Calculate profit
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal grossProfitRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal operatingExpense = BigDecimal.ZERO; // Simplified
        BigDecimal netProfit = grossProfit.subtract(operatingExpense);
        BigDecimal netProfitRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal averageOrderValue = orderCount > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        ProfitAnalysis analysis = new ProfitAnalysis();
        analysis.setAnalysisDate(date);
        analysis.setTotalRevenue(totalRevenue);
        analysis.setFoodCost(foodCost);
        analysis.setLaborCost(laborCost);
        analysis.setOtherCost(otherCost);
        analysis.setTotalCost(totalCost);
        analysis.setGrossProfit(grossProfit);
        analysis.setGrossProfitRate(grossProfitRate);
        analysis.setOperatingExpense(operatingExpense);
        analysis.setNetProfit(netProfit);
        analysis.setNetProfitRate(netProfitRate);
        analysis.setOrderCount(orderCount);
        analysis.setCustomerCount(customerCount);
        analysis.setAverageOrderValue(averageOrderValue);
        analysis.setTenantId(tenantId);
        analysis.setCreateTime(LocalDateTime.now());
        analysis.setUpdateTime(LocalDateTime.now());

        profitAnalysisMapper.insert(analysis);
        return analysis;
    }

    /**
     * 获取 profit trend。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getProfitTrend(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<BigDecimal> profits = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.toString());

            ProfitAnalysis analysis = getProfitAnalysisByDate(current, tenantId);
            if (analysis != null) {
                revenues.add(analysis.getTotalRevenue());
                costs.add(analysis.getTotalCost());
                profits.add(analysis.getGrossProfit());
            } else {
                revenues.add(BigDecimal.ZERO);
                costs.add(BigDecimal.ZERO);
                profits.add(BigDecimal.ZERO);
            }

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("revenues", revenues);
        result.put("costs", costs);
        result.put("profits", profits);

        return result;
    }

    /**
     * 获取 profit structure。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getProfitStructure(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        List<ProfitAnalysis> analyses = getProfitAnalysisList(startDate, endDate, tenantId);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalFoodCost = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        BigDecimal totalOtherCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (ProfitAnalysis analysis : analyses) {
            totalRevenue = totalRevenue.add(analysis.getTotalRevenue() != null ? analysis.getTotalRevenue() : BigDecimal
                    .ZERO);
            totalFoodCost = totalFoodCost.add(analysis.getFoodCost() != null ? analysis.getFoodCost() : BigDecimal
                    .ZERO);
            totalLaborCost = totalLaborCost.add(analysis.getLaborCost() != null ? analysis.getLaborCost() : BigDecimal
                    .ZERO);
            totalOtherCost = totalOtherCost.add(analysis.getOtherCost() != null ? analysis.getOtherCost() : BigDecimal
                    .ZERO);
            totalProfit = totalProfit.add(analysis.getGrossProfit() != null ? analysis.getGrossProfit() : BigDecimal
                    .ZERO);
        }

        result.put("totalRevenue", totalRevenue);
        result.put("totalFoodCost", totalFoodCost);
        result.put("totalLaborCost", totalLaborCost);
        result.put("totalOtherCost", totalOtherCost);
        result.put("totalProfit", totalProfit);

        return result;
    }

    // ==================== Statistics ====================

    /**
     * 获取 finance statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getFinanceStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // Withdrawal statistics
        LambdaQueryWrapper<WithdrawalApplication> withdrawalQw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            withdrawalQw.ge(WithdrawalApplication::getCreateTime, startDate);
        }
        if (endDate != null) {
            withdrawalQw.le(WithdrawalApplication::getCreateTime, endDate);
        }
        if (tenantId != null) {
            withdrawalQw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        List<WithdrawalApplication> withdrawals = withdrawalMapper.selectList(withdrawalQw);

        BigDecimal totalWithdrawal = BigDecimal.ZERO;
        int pendingCount = 0;
        int approvedCount = 0;
        int paidCount = 0;

        for (WithdrawalApplication withdrawal : withdrawals) {
            totalWithdrawal = totalWithdrawal.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal
                    .ZERO);
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PENDING) pendingCount++;
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_APPROVED) approvedCount++;
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PAID) paidCount++;
        }

        result.put("totalWithdrawal", totalWithdrawal);
        result.put("totalApplications", withdrawals.size());
        result.put("pendingCount", pendingCount);
        result.put("approvedCount", approvedCount);
        result.put("paidCount", paidCount);

        return result;
    }

    /**
     * 获取 withdrawal statistics。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getWithdrawalStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<WithdrawalApplication> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        List<WithdrawalApplication> withdrawals = withdrawalMapper.selectList(qw);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        Map<Integer, Integer> statusCountMap = new HashMap<>();

        for (WithdrawalApplication withdrawal : withdrawals) {
            totalAmount = totalAmount.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal.ZERO);
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PAID) {
                paidAmount = paidAmount.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal.ZERO);
            }
            statusCountMap.merge(withdrawal.getStatus(), 1, Integer::sum);
        }

        result.put("totalAmount", totalAmount);
        result.put("paidAmount", paidAmount);
        result.put("statusCount", statusCountMap);
        // 修改点：追加前端预期字段别名（P2-14）
        Integer total = 0;
        for (Integer count : statusCountMap.values()) {
            total += count;
        }
        result.put("totalApplications", total);
        result.put("pendingCount", statusCountMap.getOrDefault(WithdrawalApplication.STATUS_PENDING, 0));
        result.put("paidCount", statusCountMap.getOrDefault(WithdrawalApplication.STATUS_PAID, 0));
        result.put("totalWithdrawal", result.getOrDefault("totalAmount", BigDecimal.ZERO));

        return result;
    }

    // ==================== Private Methods ====================

    private String generateApplicationNo() {
        return "WD" + System.currentTimeMillis();
    }

    /**
     * 类型安全的 BigDecimal 取值：null 返回 ZERO；BigDecimal 直接返回；其他 Number 走 toString 构造，
     * 避免 (BigDecimal) 强转 ClassCastException 与 new BigDecimal(doubleValue()) 精度陷阱。
     */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            try {
                return new BigDecimal(val.toString());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}


