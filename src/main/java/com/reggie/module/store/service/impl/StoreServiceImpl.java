package com.reggie.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.PasswordUtils;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.order.model.Orders;
import com.reggie.module.user.model.User;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.user.mapper.UserMapper;
import com.reggie.module.store.mapper.StoreDailySummaryMapper;
import com.reggie.module.store.mapper.StoreEmployeePermissionMapper;
import com.reggie.module.store.mapper.StoreInfoMapper;
import com.reggie.module.store.model.StoreDailySummary;
import com.reggie.module.store.model.StoreEmployeePermission;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.model.StoreSearchDTO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.reggie.module.store.dto.UpdateStoreDTO;
import com.reggie.module.store.service.StoreService;
import com.reggie.common.LogMaskUtils;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.user.service.UserService;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店管理服务实现
 * 提供总部-分店模式下的门店全生命周期管理和数据隔离
 * 修改点：新增分页搜索、编辑、批量操作、导出方法
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class StoreServiceImpl implements StoreService {

    /** 门店信息Mapper */
    @Autowired
    private StoreInfoMapper storeInfoMapper;
    /** 门店日报汇总Mapper */
    @Autowired
    private StoreDailySummaryMapper summaryMapper;
    /** 门店员工权限Mapper */
    @Autowired
    private StoreEmployeePermissionMapper permissionMapper;

    /** 租户服务 */
    @Autowired
    private TenantService tenantService;
    /** 员工服务 */
    @Autowired
    private EmployeeService employeeService;
    /** 订单服务 */
    @Autowired
    private OrderService orderService;
    /** 用户服务 */
    @Autowired
    private UserService userService;
    /** 订单 Mapper（按门店聚合用） */
    @Autowired
    private OrderMapper orderMapper;
    /** 用户 Mapper（按门店聚合用） */
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreInfo createStore(StoreInfo storeInfo, Tenant tenant,
                                  String username, String password) {
        // 1. 创建租户
        tenant.setStatus(1);
        tenantService.save(tenant);

        // 2. 创建门店信息
        storeInfo.setTenantId(tenant.getId());
        storeInfo.setParentTenantId(BaseContext.getCurrentTenantId()); // 总部ID
        storeInfoMapper.insert(storeInfo);

        // 3. 创建门店管理员账号（加密密码）
        Employee employee = new Employee();
        employee.setName(username);
        employee.setUsername(username);
        employee.setPassword(PasswordUtils.encodePassword(password));
        employee.setStatus(1);
        employee.setTenantId(tenant.getId());
        // 修复：门店联系人手机号作为管理员手机号，避免employee.phone为NULL导致插入失败
        employee.setPhone(storeInfo.getContactPhone() != null
                ? storeInfo.getContactPhone() : tenant.getPhone());
        employee.setSex("1");
        employee.setIdNumber("");
        employeeService.save(employee);

        // 4. 分配店长权限
        StoreEmployeePermission permission = new StoreEmployeePermission();
        permission.setEmployeeId(employee.getId());
        permission.setTenantId(tenant.getId());
        permission.setRoleType(StoreEmployeePermission.ROLE_MANAGER);
        permission.setPermissions("[\"dish:view\",\"dish:edit\",\"order:view\",\"order:process\"," +
                "\"report:view\",\"employee:manage\",\"setmeal:view\",\"setmeal:edit\"]");
        permission.setIsActive(1);
        permissionMapper.insert(permission);

        log.info("[门店管理] 创建门店成功: {} (tenantId={})", storeInfo.getStoreCode(), tenant.getId());
        return storeInfo;
    }

    // 修改点：使用白名单 DTO + UpdateWrapper 替代 Map + updateById()，防止 mass assignment 攻击
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStore(Long tenantId, UpdateStoreDTO updateDTO) {
        // 1. 更新 Tenant 表（白名单字段：name, address, status）
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            log.warn("[门店管理] 编辑门店失败: tenantId={} 不存在", tenantId);
            return;
        }
        // 租户权限校验：防止跨租户越权编辑门店
        StoreInfo existingStore = storeInfoMapper.findByTenantId(tenantId);
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && existingStore != null
                && !currentTenantId.equals(existingStore.getParentTenantId())
                && !currentTenantId.equals(tenantId)) {
            log.warn("[门店管理] 无权编辑其他门店: tenantId={}, currentTenantId={}", tenantId, currentTenantId);
            return;
        }

        // 使用 UpdateWrapper，仅更新白名单字段（passwordType 等敏感字段不可被覆盖）
        UpdateWrapper<Tenant> tenantWrapper = new UpdateWrapper<>();
        tenantWrapper.eq("id", tenantId);
        if (updateDTO.getStoreName() != null) {
            tenantWrapper.set("name", updateDTO.getStoreName());
        }
        if (updateDTO.getAddress() != null) {
            tenantWrapper.set("address", updateDTO.getAddress());
        }
        if (updateDTO.getStatus() != null) {
            tenantWrapper.set("status", updateDTO.getStatus());
        }
        if (updateDTO.getStoreName() != null || updateDTO.getAddress() != null || updateDTO.getStatus() != null) {
            tenantService.update(tenantWrapper);
        }

        // 2. 更新 StoreInfo 表（白名单字段，parentTenantId 等敏感字段不可被覆盖）
        StoreInfo storeInfo = storeInfoMapper.findByTenantId(tenantId);
        if (storeInfo != null) {
            UpdateWrapper<StoreInfo> storeWrapper = new UpdateWrapper<>();
            storeWrapper.eq("tenant_id", tenantId);
            if (updateDTO.getStoreCode() != null) {
                storeWrapper.set("store_code", updateDTO.getStoreCode());
            }
            if (updateDTO.getStoreType() != null) {
                storeWrapper.set("store_type", updateDTO.getStoreType());
            }
            if (updateDTO.getBusinessHours() != null) {
                storeWrapper.set("business_hours", updateDTO.getBusinessHours());
            }
            if (updateDTO.getContactPerson() != null) {
                storeWrapper.set("contact_person", updateDTO.getContactPerson());
            }
            if (updateDTO.getContactPhone() != null) {
                storeWrapper.set("contact_phone", updateDTO.getContactPhone());
            }
            if (updateDTO.getDeliveryRadius() != null) {
                storeWrapper.set("delivery_radius", updateDTO.getDeliveryRadius());
            }
            if (updateDTO.getMinDeliveryAmount() != null) {
                storeWrapper.set("min_delivery_amount", updateDTO.getMinDeliveryAmount());
            }
            if (updateDTO.getDeliveryFee() != null) {
                storeWrapper.set("delivery_fee", updateDTO.getDeliveryFee());
            }
            if (updateDTO.getIsDeliveryEnabled() != null) {
                storeWrapper.set("is_delivery_enabled", updateDTO.getIsDeliveryEnabled());
            }
            if (updateDTO.getIsDineInEnabled() != null) {
                storeWrapper.set("is_dine_in_enabled", updateDTO.getIsDineInEnabled());
            }
            // 使用 UpdateWrapper 的 set() 值更新；entity 传 null 避免 MP 从旧实体读取字段值覆盖 set() 结果
            storeInfoMapper.update(null, storeWrapper);

            log.info("[门店管理] 编辑门店成功: tenantId={}, storeCode={}, contactPhone={}",
                    tenantId, updateDTO.getStoreCode(), LogMaskUtils.maskPhone(updateDTO.getContactPhone()));
            return;
        }

        log.info("[门店管理] 编辑门店成功: tenantId={}", tenantId);
    }

    @Override
    public List<Map<String, Object>> listAllStores() {
        List<StoreInfo> stores = storeInfoMapper.selectList(null);

        // 修改点：消除 N+1。一次性批量加载租户与今日日报汇总，避免逐店 getById / getTodaySummaryObj
        Map<Long, String> tenantNameMap = new HashMap<>();
        Map<Long, Integer> tenantStatusMap = new HashMap<>();
        for (Tenant t : tenantService.list()) {
            tenantNameMap.put(t.getId(), t.getName());
            tenantStatusMap.put(t.getId(), t.getStatus());
        }

        Map<Long, StoreDailySummary> summaryMap = new HashMap<>();
        LambdaQueryWrapper<StoreDailySummary> sumWrapper = new LambdaQueryWrapper<>();
        sumWrapper.eq(StoreDailySummary::getSummaryDate, LocalDate.now());
        for (StoreDailySummary s : summaryMapper.selectList(sumWrapper)) {
            summaryMap.put(s.getTenantId(), s);
        }

        return stores.stream().map(si -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", si.getId());
            map.put("tenantId", si.getTenantId());
            map.put("storeCode", si.getStoreCode());
            map.put("storeType", si.getStoreType());
            map.put("parentTenantId", si.getParentTenantId());

            map.put("storeName", tenantNameMap.getOrDefault(si.getTenantId(), ""));
            map.put("contactPhone", si.getContactPhone());
            map.put("contactPerson", si.getContactPerson());

            StoreDailySummary summary = summaryMap.get(si.getTenantId());
            if (summary != null) {
                map.put("todayOrders", summary.getTotalOrders());
                map.put("todayAmount", summary.getActualAmount());
            } else {
                map.put("todayOrders", 0);
                map.put("todayAmount", BigDecimal.ZERO);
            }

            map.put("status", tenantStatusMap.getOrDefault(si.getTenantId(), 0));
            map.put("isDeliveryEnabled", si.getIsDeliveryEnabled());
            map.put("isDineInEnabled", si.getIsDineInEnabled());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    // 修改点：新增分页搜索方法
    @Override
    public Map<String, Object> searchStores(StoreSearchDTO dto) {
        Page<Map<String, Object>> page = PageUtils.of(dto.getPage(), dto.getPageSize());
        // 防止 SQL 注入：sortOrder 仅允许 asc/desc
        String sortOrder = "desc";
        if (dto.getSortOrder() != null && "asc".equalsIgnoreCase(dto.getSortOrder())) {
            sortOrder = "asc";
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        IPage<Map<String, Object>> result = storeInfoMapper.searchStores(
                page, dto.getKeyword(), dto.getStoreType(), dto.getStatus(),
                dto.getSortBy(), sortOrder, tenantId);

        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("records", result.getRecords());
        pageResult.put("total", result.getTotal());
        pageResult.put("pages", result.getPages());
        pageResult.put("current", result.getCurrent());
        pageResult.put("size", result.getSize());
        return pageResult;
    }

    // 修改点：新增详情方法
    @Override
    public Map<String, Object> getStoreDetail(Long tenantId) {
        return storeInfoMapper.searchStoreDetail(tenantId);
    }

    @Override
    public List<StoreInfo> listBranchStores(Long parentTenantId) {
        if (parentTenantId == null) return Collections.emptyList();
        return storeInfoMapper.findByParentTenantId(parentTenantId);
    }

    @Override
    public Map<String, Object> switchStore(Long targetTenantId) {
        if (targetTenantId == null) return Collections.emptyMap();

        // 越权校验：以当前员工的“归属门店”为基准判断权限
        Long currentUserId = BaseContext.getCurrentId();
        Long homeTenantId = null;
        if (currentUserId != null) {
            Employee currentEmployee = employeeService.getById(currentUserId);
            if (currentEmployee != null && currentEmployee.getTenantId() != null) {
                homeTenantId = currentEmployee.getTenantId();
            }
        }
        // 兜底：取不到员工归属门店时使用当前上下文租户
        if (homeTenantId == null) {
            homeTenantId = BaseContext.getCurrentTenantId();
        }

        StoreInfo targetStore = storeInfoMapper.findByTenantId(targetTenantId);
        if (targetStore == null) {
            throw new CustomException("目标门店不存在，无法切换");
        }

        // 仅允许切换到自身门店或总店管理员切换到下属分店；分店管理员禁止切换到非所属门店
        if (!targetTenantId.equals(homeTenantId)) {
            StoreInfo homeStore = storeInfoMapper.findByTenantId(homeTenantId);
            boolean isHeadquarters = homeStore != null
                    && (homeStore.getParentTenantId() == null
                            || StoreInfo.TYPE_HEADQUARTER == homeStore.getStoreType());
            boolean isSubordinateBranch = isHeadquarters
                    && homeTenantId.equals(targetStore.getParentTenantId());
            if (!isSubordinateBranch) {
                throw new CustomException("无权切换到该门店：仅总店管理员可切换到下属分店");
            }
        }

        // 设置新门店上下文
        BaseContext.setCurrentTenantId(targetTenantId);

        Tenant tenant = tenantService.getById(targetTenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (targetStore != null) {
            result.put("tenantId", targetTenantId);
            result.put("storeCode", targetStore.getStoreCode());
            result.put("storeType", targetStore.getStoreType());
            result.put("businessHours", targetStore.getBusinessHours());
        }
        if (tenant != null) {
            result.put("storeName", tenant.getName());
            result.put("status", tenant.getStatus());
        }

        log.info("[门店切换] 当前门店切换至: tenantId={}, 操作人={}, 归属门店={}",
                targetTenantId, currentUserId, homeTenantId);
        return result;
    }

    @Override
    public Map<String, Object> getTodaySummary(Long tenantId) {
        if (tenantId == null) tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) return Collections.emptyMap();

        Map<String, Object> summary = new LinkedHashMap<>();

        // 修改点：改用 count 聚合 + 仅查已完成订单金额，避免 list 全量载入今日订单到内存（消除 OOM 风险）
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long totalOrders = orderService.count(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getTenantId, tenantId).ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end));
        long completed = orderService.count(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getTenantId, tenantId).ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end)
                .eq(Orders::getStatus, Orders.STATUS_COMPLETED));
        long cancelled = orderService.count(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getTenantId, tenantId).ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end)
                .eq(Orders::getStatus, Orders.STATUS_CANCELLED));
        BigDecimal totalAmount = orderService.list(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getTenantId, tenantId).ge(Orders::getCreateTime, start).lt(Orders::getCreateTime, end)
                .eq(Orders::getStatus, Orders.STATUS_COMPLETED))
                .stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.put("totalOrders", totalOrders);
        summary.put("completedOrders", completed);
        summary.put("cancelledOrders", cancelled);
        summary.put("todayAmount", totalAmount);

        // 今日新增用户（按租户隔离）
        summary.put("newUsers", userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .ge(User::getCreateTime, start)
                .lt(User::getCreateTime, end)));

        return summary;
    }

    @Override
    public StoreDailySummary getYesterdaySummary(Long tenantId) {
        if (tenantId == null) tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) return null;

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LambdaQueryWrapper<StoreDailySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreDailySummary::getTenantId, tenantId)
               .eq(StoreDailySummary::getSummaryDate, yesterday);
        return summaryMapper.selectOne(wrapper);
    }

    @Override
    public void updateStoreStatus(Long tenantId, Integer status) {
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            return;
        }
        // 租户权限校验：防止跨租户越权修改门店状态
        Long currentTenantId = BaseContext.getCurrentTenantId();
        StoreInfo existingStore = storeInfoMapper.findByTenantId(tenantId);
        if (currentTenantId != null && existingStore != null && !currentTenantId.equals(existingStore.getParentTenantId()) && !currentTenantId.equals(tenantId)) {
            log.warn("[门店管理] 无权修改其他门店状态: tenantId={}, currentTenantId={}", tenantId, currentTenantId);
            return;
        }
        // 使用 UpdateWrapper 仅更新 status 字段，防止 mass assignment
        UpdateWrapper<Tenant> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", tenantId).set("status", status);
        tenantService.update(wrapper);
        log.info("[门店管理] 门店{}状态更新为: {}", tenantId, status);
    }

    // 修改点：新增批量更新状态方法
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStoreStatus(List<Long> tenantIds, Integer status) {
        int successCount = 0;
        Long currentTenantId = BaseContext.getCurrentTenantId();
        for (Long tenantId : tenantIds) {
            try {
                Tenant tenant = tenantService.getById(tenantId);
                if (tenant == null) continue;
                // 租户权限校验：防止跨租户批量修改状态
                StoreInfo existingStore = storeInfoMapper.findByTenantId(tenantId);
                if (currentTenantId != null && existingStore != null && !currentTenantId.equals(existingStore.getParentTenantId()) && !currentTenantId.equals(tenantId)) {
                    log.warn("[门店管理] 批量更新跳过无权门店: tenantId={}, currentTenantId={}", tenantId, currentTenantId);
                    continue;
                }
                // 使用 UpdateWrapper 仅更新 status 字段，防止 mass assignment
                UpdateWrapper<Tenant> wrapper = new UpdateWrapper<>();
                wrapper.eq("id", tenantId).set("status", status);
                tenantService.update(wrapper);
                successCount++;
            } catch (Exception e) {
                log.error("[门店管理] 批量更新状态失败: tenantId={}, error={}", tenantId, e.getMessage(), e);
            }
        }
        log.info("[门店管理] 批量更新门店状态完成: 成功{}个, 目标状态={}", successCount, status);
        return successCount;
    }

    // 修改点：新增导出方法
    @Override
    public List<Map<String, Object>> exportStores(String keyword, Integer storeType, Integer status) {
        // fail-closed：租户缺失时拒绝导出。若透传 null 给 XML 的 si.tenant_id = #{tenantId}，
        // SQL 中 `= NULL` 恒为 unknown，会静默返回空集（fail-open 空结果）。
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.reggie.common.CustomException("无导出权限，租户上下文缺失");
        }
        return storeInfoMapper.exportStores(keyword, storeType, status, tenantId);
    }

    @Override
    public Map<String, Object> getAggregatedDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // 修改点：消除 N+1。一次性加载门店/租户，并用按门店分组的聚合 SQL 替代逐店 getTodaySummary 循环
        List<StoreInfo> storeInfos = storeInfoMapper.selectList(null);
        dashboard.put("totalStores", storeInfos.size());

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 一次性批量聚合：今日订单数/已完成金额（按门店）、今日新增用户（按门店）
        Map<Long, Integer> ordersByTenant = new HashMap<>();
        Map<Long, BigDecimal> amountByTenant = new HashMap<>();
        for (Map<String, Object> row : orderMapper.statTodayByTenant(start, end, Orders.STATUS_COMPLETED)) {
            if (row.get("tenantId") == null) {
                continue;
            }
            Long tid = ((Number) row.get("tenantId")).longValue();
            int orders = row.get("totalOrders") == null ? 0 : ((Number) row.get("totalOrders")).intValue();
            BigDecimal amt = row.get("todayAmount") == null
                    ? BigDecimal.ZERO : new BigDecimal(row.get("todayAmount").toString());
            ordersByTenant.put(tid, orders);
            amountByTenant.put(tid, amt);
        }

        Map<Long, Integer> newUsersByTenant = new HashMap<>();
        for (Map<String, Object> row : userMapper.statNewUsersByTenant(start, end)) {
            if (row.get("tenantId") == null) {
                continue;
            }
            Long tid = ((Number) row.get("tenantId")).longValue();
            int nu = row.get("newUsers") == null ? 0 : ((Number) row.get("newUsers")).intValue();
            newUsersByTenant.put(tid, nu);
        }

        // 一次性加载租户名称，避免逐店 getById 的 N+1
        Map<Long, String> tenantNameMap = new HashMap<>();
        for (Tenant t : tenantService.list()) {
            tenantNameMap.put(t.getId(), t.getName());
        }

        int totalOrders = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalNewUsers = 0;
        List<Map<String, Object>> ranking = new ArrayList<>();

        for (StoreInfo si : storeInfos) {
            Long tenantId = si.getTenantId();
            int storeOrders = ordersByTenant.getOrDefault(tenantId, 0);
            BigDecimal storeAmount = amountByTenant.getOrDefault(tenantId, BigDecimal.ZERO);
            int storeNewUsers = newUsersByTenant.getOrDefault(tenantId, 0);

            totalOrders += storeOrders;
            totalAmount = totalAmount.add(storeAmount);
            totalNewUsers += storeNewUsers;

            Map<String, Object> rank = new LinkedHashMap<>();
            rank.put("tenantId", tenantId);
            rank.put("storeCode", si.getStoreCode());
            rank.put("storeName", tenantNameMap.getOrDefault(tenantId, ""));
            rank.put("todayOrders", storeOrders);
            rank.put("todayAmount", storeAmount);
            ranking.add(rank);
        }

        dashboard.put("todayTotalOrders", totalOrders);
        dashboard.put("todayTotalAmount", totalAmount);
        dashboard.put("todayNewUsers", totalNewUsers);

        if (totalOrders > 0) {
            dashboard.put("avgOrderAmount",
                    totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
        }

        // 门店排行（按今日订单量降序）
        ranking.sort((a, b) -> Integer.compare(
                ((Number) b.getOrDefault("todayOrders", 0)).intValue(),
                ((Number) a.getOrDefault("todayOrders", 0)).intValue()));
        dashboard.put("storeRanking", ranking);

        return dashboard;
    }

    @Override
    public Map<String, Object> getStoreStats() {
        // 单条 SQL 聚合统计，替代前端 listAllStores 拉全量 + filter，消除 N+1 与全量内存计算
        Map<String, Object> stats = storeInfoMapper.statStores(LocalDate.now());
        if (stats == null) {
            stats = new LinkedHashMap<>();
            stats.put("totalStores", 0);
            stats.put("activeStores", 0);
            stats.put("inactiveStores", 0);
            stats.put("todayTotalStores", 0);
        }
        return stats;
    }

    private StoreDailySummary getTodaySummaryObj(Long tenantId) {
        LambdaQueryWrapper<StoreDailySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreDailySummary::getTenantId, tenantId)
               .eq(StoreDailySummary::getSummaryDate, LocalDate.now());
        return summaryMapper.selectOne(wrapper);
    }

    @Override
    public StoreInfo findByTenantId(Long tenantId) {
        return storeInfoMapper.findByTenantId(tenantId);
    }
}







