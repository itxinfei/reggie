package com.reggie.module.subsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.subsidy.dto.SubsidyGrantDTO;
import com.reggie.module.subsidy.mapper.MealSubsidyAccountMapper;
import com.reggie.module.subsidy.mapper.MealSubsidyRecordMapper;
import com.reggie.module.subsidy.model.MealSubsidyAccount;
import com.reggie.module.subsidy.model.MealSubsidyRecord;
import com.reggie.module.subsidy.service.SubsidyService;
import com.reggie.module.subsidy.vo.SubsidyAccountVO;
import com.reggie.module.sys.model.Department;
import com.reggie.module.sys.service.DepartmentService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 餐补 Service 实现
 * <p>
 * 余额变动一律走 {@link MealSubsidyAccountMapper} 的原子 UPDATE（受影响行数判定成败），
 * 成功后同事务写 {@link MealSubsidyRecord} 流水；流水唯一键冲突按幂等成功处理。
 * 发放/核销/回充三类均满足：① 原子 ② 流水 ③ 幂等。
 * </p>
 */
@Slf4j
@Service
public class SubsidyServiceImpl extends ServiceImpl<MealSubsidyAccountMapper, MealSubsidyAccount>
        implements SubsidyService {

    @Autowired
    private MealSubsidyAccountMapper accountMapper;

    @Autowired
    private MealSubsidyRecordMapper recordMapper;

    @Autowired
    private com.reggie.module.subsidy.mapper.SubsidyStatMapper statMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentService departmentService;

    @Override
    public int grant(SubsidyGrantDTO dto, Long tenantId, Long operatorId) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("发放金额必须大于0");
        }
        List<Long> userIds = resolveGrantTargets(dto, tenantId);
        if (userIds.isEmpty()) {
            throw new CustomException("未找到可发放的用户（部门下无用户或用户不存在）");
        }
        int granted = 0;
        for (Long userId : userIds) {
            if (grantToOne(tenantId, userId, dto.getAmount(), operatorId, dto.getRemark())) {
                granted++;
            }
        }
        log.info("[餐补] 发放完成: tenant={}, 目标人数={}, 实际发放={}, 金额={}",
                tenantId, userIds.size(), granted, dto.getAmount());
        return granted;
    }

    /** 单人发放：确保账户存在 → 原子入账 → 记 GRANT 流水。 */
    private boolean grantToOne(Long tenantId, Long userId, BigDecimal amount, Long operatorId, String remark) {
        MealSubsidyAccount account = ensureAccount(tenantId, userId, null);
        int affected = accountMapper.grantCredit(tenantId, userId, amount);
        if (affected == 0) {
            log.warn("[餐补] 发放跳过（账户冻结或不存在）: tenant={}, user={}", tenantId, userId);
            return false;
        }
        insertRecord(tenantId, userId, account.getId(), MealSubsidyRecord.TYPE_GRANT,
                amount, null, null, operatorId, remark);
        return true;
    }

    @Override
    public void consumeForOrder(Long userId, Long tenantId, Long orderId, BigDecimal amount, String tradeNo) {
        // 幂等先行：同 tradeNo 的核销流水已存在则直接返回（不重复扣款）
        if (recordExists(tradeNo, MealSubsidyRecord.TYPE_CONSUME)) {
            log.info("[餐补] 核销幂等跳过: tradeNo={}, orderId={}", tradeNo, orderId);
            return;
        }
        int affected = accountMapper.consumeDeduct(tenantId, userId, amount);
        if (affected == 0) {
            throw new CustomException("餐补余额不足或账户不可用，无法使用餐补支付");
        }
        MealSubsidyAccount account = getOwnedAccount(tenantId, userId);
        insertRecord(tenantId, userId, account.getId(), MealSubsidyRecord.TYPE_CONSUME,
                amount, orderId, tradeNo, null, "订单核销");
    }

    @Override
    public void refundForOrder(Long tenantId, Long userId, Long orderId, BigDecimal amount, String refundNo) {
        if (recordExists(refundNo, MealSubsidyRecord.TYPE_REFUND)) {
            log.info("[餐补] 回充幂等跳过: refundNo={}, orderId={}", refundNo, orderId);
            return;
        }
        int affected = accountMapper.refundCredit(tenantId, userId, amount);
        if (affected == 0) {
            // 账户被冻结/删除时回充失败：记录错误日志交人工处理，不阻断退款主流程
            log.error("[餐补] 回充失败（账户冻结或不存在），需人工核查: tenant={}, user={}, order={}, amount={}",
                    tenantId, userId, orderId, amount);
            return;
        }
        MealSubsidyAccount account = getOwnedAccount(tenantId, userId);
        insertRecord(tenantId, userId, account.getId(), MealSubsidyRecord.TYPE_REFUND,
                amount, orderId, refundNo, null, "订单退款回充");
    }

    @Override
    public MealSubsidyAccount getMyAccount(Long userId, Long tenantId) {
        return this.getOne(new LambdaQueryWrapper<MealSubsidyAccount>()
                .eq(MealSubsidyAccount::getTenantId, tenantId)
                .eq(MealSubsidyAccount::getUserId, userId)
                .eq(MealSubsidyAccount::getIsDeleted, 0));
    }

    @Override
    public IPage<SubsidyAccountVO> pageAccounts(Long tenantId, int page, int pageSize,
                                                Long departmentId, String phone) {
        // 手机号先查用户ID（user.phone 有模糊需求，避免 account 表关联查询）
        List<Long> userIdFilter = null;
        if (phone != null && !phone.isEmpty()) {
            List<Long> ids = userIdsByPhone(tenantId, phone);
            userIdFilter = ids;
            if (ids.isEmpty()) {
                return new Page<>(page, pageSize);
            }
        }
        Page<MealSubsidyAccount> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<MealSubsidyAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MealSubsidyAccount::getTenantId, tenantId)
               .eq(MealSubsidyAccount::getIsDeleted, 0)
               .eq(departmentId != null, MealSubsidyAccount::getDepartmentId, departmentId)
               .in(userIdFilter != null, MealSubsidyAccount::getUserId, userIdFilter)
               .orderByDesc(MealSubsidyAccount::getUpdateTime);
        this.page(pageInfo, wrapper);

        // 批量补用户与部门信息（避免行级 N+1）
        List<MealSubsidyAccount> records = pageInfo.getRecords();
        Map<Long, User> userMap = loadUsers(tenantId, records);
        Map<Long, String> deptNames = loadDepartmentNames(records);
        IPage<SubsidyAccountVO> voPage = pageInfo.convert(this::toVO);
        for (SubsidyAccountVO vo : voPage.getRecords()) {
            User u = userMap.get(vo.getUserId());
            if (u != null) {
                vo.setUserName(u.getName());
                vo.setPhone(u.getPhone());
            }
            vo.setDepartmentName(deptNames.get(vo.getDepartmentId()));
        }
        return voPage;
    }

    @Override
    public IPage<MealSubsidyRecord> pageRecords(Long tenantId, int page, int pageSize,
                                                Long userId, String recordType) {
        Page<MealSubsidyRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<MealSubsidyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MealSubsidyRecord::getTenantId, tenantId)
               .eq(userId != null, MealSubsidyRecord::getUserId, userId)
               .eq(recordType != null && !recordType.isEmpty(), MealSubsidyRecord::getRecordType, recordType)
               .orderByDesc(MealSubsidyRecord::getId);
        return recordMapper.selectPage(pageInfo, wrapper);
    }

    @Override
    public java.util.Map<String, Object> stats(Long tenantId) {
        java.util.Map<String, Object> raw = accountMapper.selectStats(tenantId);
        return raw != null ? raw : new java.util.HashMap<>();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> departmentReconciliation(Long tenantId,
                                                                                  String startDate, String endDate) {
        // 日期区间：[start 00:00:00, end+1d 00:00:00)，endDate 含当天
        java.time.LocalDate start;
        java.time.LocalDate end;
        try {
            start = java.time.LocalDate.parse(startDate);
            end = java.time.LocalDate.parse(endDate);
        } catch (Exception e) {
            throw new CustomException("日期格式应为 yyyy-MM-dd");
        }
        if (end.isBefore(start)) {
            throw new CustomException("结束日期不能早于起始日期");
        }
        String startTs = start.atStartOfDay().toString().replace('T', ' ');
        String endTs = end.plusDays(1).atStartOfDay().toString().replace('T', ' ');

        // 两路聚合 → 以部门维度合并（null 部门归入"未分配"）
        Map<Long, java.util.Map<String, Object>> byDept = new LinkedHashMap<>();
        for (Map<String, Object> row : statMapper.aggregateOrdersByDepartment(tenantId, startTs, endTs)) {
            Long deptId = row.get("departmentId") == null ? null : ((Number) row.get("departmentId")).longValue();
            java.util.Map<String, Object> agg = byDept.computeIfAbsent(deptId, this::newAggRow);
            agg.put("orderCount", ((Number) row.get("orderCount")).intValue());
            agg.put("orderAmount", toDecimal(row.get("orderAmount")));
        }
        for (Map<String, Object> row : statMapper.aggregateRecordsByDepartment(tenantId, startTs, endTs)) {
            Long deptId = row.get("departmentId") == null ? null : ((Number) row.get("departmentId")).longValue();
            java.util.Map<String, Object> agg = byDept.computeIfAbsent(deptId, this::newAggRow);
            BigDecimal total = toDecimal(row.get("total"));
            if (MealSubsidyRecord.TYPE_GRANT.equals(String.valueOf(row.get("recordType")))) {
                agg.put("subsidyGranted", total);
            } else {
                agg.put("subsidyUsed", total);
            }
        }

        // 补部门名；自付 = 订单金额 - 餐补核销（当前餐补为全额支付，核销即餐补承担部分）
        List<java.util.Map<String, Object>> rows = new ArrayList<>(byDept.values());
        List<Long> deptIds = new ArrayList<>();
        for (java.util.Map<String, Object> row : rows) {
            if (row.get("departmentId") != null) {
                deptIds.add((Long) row.get("departmentId"));
            }
        }
        Map<Long, String> deptNames = new LinkedHashMap<>();
        if (!deptIds.isEmpty()) {
            for (Department d : departmentService.listByIds(deptIds)) {
                deptNames.put(d.getId(), d.getName());
            }
        }
        for (java.util.Map<String, Object> row : rows) {
            Long deptId = (Long) row.get("departmentId");
            row.put("departmentName", deptId == null ? "未分配" : deptNames.getOrDefault(deptId, "已删除部门#" + deptId));
            BigDecimal orderAmount = (BigDecimal) row.get("orderAmount");
            BigDecimal subsidyUsed = (BigDecimal) row.get("subsidyUsed");
            row.put("selfPay", orderAmount.subtract(subsidyUsed));
        }
        rows.sort((a, b) -> String.valueOf(a.get("departmentName")).compareTo(String.valueOf(b.get("departmentName"))));
        return rows;
    }

    private java.util.Map<String, Object> newAggRow(Long deptId) {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("departmentId", deptId);
        row.put("orderCount", 0);
        row.put("orderAmount", BigDecimal.ZERO);
        row.put("subsidyGranted", BigDecimal.ZERO);
        row.put("subsidyUsed", BigDecimal.ZERO);
        return row;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO
                : (value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value)));
    }

    // ==================== 私有原语 ====================

    /** 解析发放目标：departmentId 优先（该部门全部正常用户），否则单用户。 */
    private List<Long> resolveGrantTargets(SubsidyGrantDTO dto, Long tenantId) {
        if (dto.getDepartmentId() != null) {
            List<Long> ids = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .eq(User::getTenantId, tenantId)
                            .eq(User::getDepartmentId, dto.getDepartmentId())
                            .eq(User::getStatus, 1))
                    .stream().map(User::getId).collect(java.util.stream.Collectors.toList());
            if (ids.isEmpty()) {
                throw new CustomException("该部门下暂无用户，请先在用户管理中为顾客分配部门");
            }
            return ids;
        }
        if (dto.getUserId() != null) {
            User user = userMapper.selectById(dto.getUserId());
            if (user == null || !tenantId.equals(user.getTenantId())) {
                throw new CustomException("用户不存在或不属于当前租户（id=" + dto.getUserId() + "）");
            }
            return Collections.singletonList(dto.getUserId());
        }
        throw new CustomException("请指定发放对象（userId 或 departmentId 二选一）");
    }

    /** 账户不存在则创建（唯一键兜底并发），返回账户。 */
    private MealSubsidyAccount ensureAccount(Long tenantId, Long userId, Long departmentId) {
        MealSubsidyAccount existing = getOwnedAccount(tenantId, userId);
        if (existing != null) {
            return existing;
        }
        MealSubsidyAccount account = new MealSubsidyAccount();
        account.setTenantId(tenantId);
        account.setUserId(userId);
        account.setDepartmentId(departmentId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalGranted(BigDecimal.ZERO);
        account.setTotalUsed(BigDecimal.ZERO);
        account.setStatus(1);
        account.setIsDeleted(0);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            // 并发首充：唯一键冲突说明对方已建，回落为查询
        }
        return getOwnedAccount(tenantId, userId);
    }

    private MealSubsidyAccount getOwnedAccount(Long tenantId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<MealSubsidyAccount>()
                .eq(MealSubsidyAccount::getTenantId, tenantId)
                .eq(MealSubsidyAccount::getUserId, userId)
                .eq(MealSubsidyAccount::getIsDeleted, 0));
    }

    private boolean recordExists(String tradeNo, String recordType) {
        if (tradeNo == null) {
            return false;
        }
        return recordMapper.selectCount(new LambdaQueryWrapper<MealSubsidyRecord>()
                .eq(MealSubsidyRecord::getTradeNo, tradeNo)
                .eq(MealSubsidyRecord::getRecordType, recordType)) > 0;
    }

    private void insertRecord(Long tenantId, Long userId, Long accountId, String type,
                              BigDecimal amount, Long orderId, String tradeNo, Long operatorId, String remark) {
        MealSubsidyRecord record = new MealSubsidyRecord();
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setAccountId(accountId);
        record.setRecordType(type);
        record.setAmount(amount);
        BigDecimal balanceAfter = accountMapper.selectBalance(tenantId, userId);
        record.setBalanceAfter(balanceAfter != null ? balanceAfter : BigDecimal.ZERO);
        record.setOrderId(orderId);
        record.setTradeNo(tradeNo);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 唯一键兜底：并发重复请求，幂等视为成功（余额原子操作已完成一次）
            log.warn("[餐补] 流水唯一键冲突，幂等处理: type={}, tradeNo={}", type, tradeNo);
        }
    }

    private List<Long> userIdsByPhone(Long tenantId, String phone) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getTenantId, tenantId)
                        .like(User::getPhone, phone))
                .stream().map(User::getId).collect(java.util.stream.Collectors.toList());
    }

    private Map<Long, User> loadUsers(Long tenantId, List<MealSubsidyAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = new ArrayList<>();
        for (MealSubsidyAccount a : accounts) {
            userIds.add(a.getUserId());
        }
        Map<Long, User> map = new LinkedHashMap<>();
        for (User u : userMapper.selectBatchIds(userIds)) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private Map<Long, String> loadDepartmentNames(List<MealSubsidyAccount> accounts) {
        Map<Long, String> names = new LinkedHashMap<>();
        List<Long> deptIds = new ArrayList<>();
        for (MealSubsidyAccount a : accounts) {
            if (a.getDepartmentId() != null && !names.containsKey(a.getDepartmentId())) {
                deptIds.add(a.getDepartmentId());
            }
        }
        if (deptIds.isEmpty()) {
            return names;
        }
        for (Department d : departmentService.listByIds(deptIds)) {
            names.put(d.getId(), d.getName());
        }
        return names;
    }

    private SubsidyAccountVO toVO(MealSubsidyAccount a) {
        SubsidyAccountVO vo = new SubsidyAccountVO();
        vo.setId(a.getId());
        vo.setTenantId(a.getTenantId());
        vo.setUserId(a.getUserId());
        vo.setDepartmentId(a.getDepartmentId());
        vo.setBalance(a.getBalance());
        vo.setTotalGranted(a.getTotalGranted());
        vo.setTotalUsed(a.getTotalUsed());
        vo.setStatus(a.getStatus());
        return vo;
    }
}
