package com.reggie.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.*;
import com.reggie.module.store.mapper.*;
import com.reggie.module.store.model.*;
import com.reggie.module.store.service.StoreService;
import com.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 门店管理服务实现
 * 提供总部-分店模式下的门店全生命周期管理和数据隔离
 */
@Slf4j
@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreInfoMapper storeInfoMapper;
    @Autowired
    private StoreDailySummaryMapper summaryMapper;
    @Autowired
    private StoreEmployeePermissionMapper permissionMapper;

    @Autowired
    private TenantService tenantService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public StoreInfo createStore(StoreInfo storeInfo, Tenant tenant,
                                  String username, String password) {
        // 1. 创建租户
        tenant.setStatus(1);
        tenantService.save(tenant);

        // 2. 创建门店信息
        storeInfo.setTenantId(tenant.getId());
        storeInfo.setParentTenantId(BaseContext.getCurrentTenantId()); // 总部ID
        storeInfoMapper.insert(storeInfo);

        // 3. 创建门店管理员账号
        Employee employee = new Employee();
        employee.setName(username);
        employee.setUsername(username);
        employee.setPassword(password);
        employee.setStatus(1);
        employee.setTenantId(tenant.getId());
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

    @Override
    public List<Map<String, Object>> listAllStores() {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<StoreInfo> stores;

        if (currentTenantId == null) {
            // 无租户上下文：返回所有门店
            stores = storeInfoMapper.selectList(null);
        } else {
            // 有租户上下文：返回当前门店及其分店
            StoreInfo current = storeInfoMapper.findByTenantId(currentTenantId);
            if (current != null && current.getStoreType() == StoreInfo.TYPE_HEADQUARTER) {
                // 当前为总店，返回旗下所有门店
                stores = new ArrayList<>();
                stores.add(current);
                stores.addAll(storeInfoMapper.findByParentTenantId(currentTenantId));
            } else {
                stores = Collections.singletonList(current);
            }
        }

        return stores.stream().map(si -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", si.getId());
            map.put("tenantId", si.getTenantId());
            map.put("storeCode", si.getStoreCode());
            map.put("storeType", si.getStoreType());
            map.put("parentTenantId", si.getParentTenantId());

            // 查询租户名称
            Tenant tenant = tenantService.getById(si.getTenantId());
            map.put("storeName", tenant != null ? tenant.getName() : "");
            map.put("contactPhone", si.getContactPhone());

            // 今日概况
            StoreDailySummary summary = getTodaySummaryObj(si.getTenantId());
            if (summary != null) {
                map.put("todayOrders", summary.getTotalOrders());
                map.put("todayAmount", summary.getActualAmount());
            } else {
                map.put("todayOrders", 0);
                map.put("todayAmount", BigDecimal.ZERO);
            }

            // 门店状态
            map.put("status", tenant != null ? tenant.getStatus() : 0);
            map.put("isDeliveryEnabled", si.getIsDeliveryEnabled());
            map.put("isDineInEnabled", si.getIsDineInEnabled());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<StoreInfo> listBranchStores(Long parentTenantId) {
        if (parentTenantId == null) return Collections.emptyList();
        return storeInfoMapper.findByParentTenantId(parentTenantId);
    }

    @Override
    public Map<String, Object> switchStore(Long targetTenantId) {
        if (targetTenantId == null) return Collections.emptyMap();

        // 设置新门店上下文
        BaseContext.setCurrentTenantId(targetTenantId);

        StoreInfo storeInfo = storeInfoMapper.findByTenantId(targetTenantId);
        Tenant tenant = tenantService.getById(targetTenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (storeInfo != null) {
            result.put("tenantId", targetTenantId);
            result.put("storeCode", storeInfo.getStoreCode());
            result.put("storeType", storeInfo.getStoreType());
            result.put("businessHours", storeInfo.getBusinessHours());
        }
        if (tenant != null) {
            result.put("storeName", tenant.getName());
            result.put("status", tenant.getStatus());
        }

        log.info("[门店切换] 当前门店切换至: tenantId={}", targetTenantId);
        return result;
    }

    @Override
    public Map<String, Object> getTodaySummary(Long tenantId) {
        if (tenantId == null) tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) return Collections.emptyMap();

        Map<String, Object> summary = new LinkedHashMap<>();

        // 今日订单统计
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getTenantId, tenantId)
                .ge(Orders::getCreateTime, today.atStartOfDay())
                .lt(Orders::getCreateTime, today.plusDays(1).atStartOfDay());

        List<Orders> todayOrders = orderService.list(orderWrapper);
        long completed = todayOrders.stream().filter(o -> o.getStatus() == Orders.STATUS_COMPLETED).count();
        long cancelled = todayOrders.stream().filter(o -> o.getStatus() == Orders.STATUS_CANCELLED).count();
        BigDecimal totalAmount = todayOrders.stream()
                .filter(o -> o.getStatus() == Orders.STATUS_COMPLETED)
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.put("totalOrders", todayOrders.size());
        summary.put("completedOrders", completed);
        summary.put("cancelledOrders", cancelled);
        summary.put("todayAmount", totalAmount);

        // 今日新增用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ge(User::getCreateTime, today.atStartOfDay())
                .lt(User::getCreateTime, today.plusDays(1).atStartOfDay());
        summary.put("newUsers", userService.count(userWrapper));

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
        if (tenant != null) {
            tenant.setStatus(status);
            tenantService.updateById(tenant);
            log.info("[门店管理] 门店{}状态更新为: {}", tenantId, status);
        }
    }

    @Override
    public Map<String, Object> getAggregatedDashboard() {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // 获取所有门店
        List<Map<String, Object>> stores = listAllStores();
        dashboard.put("totalStores", stores.size());

        // 汇总今日经营数据
        int totalOrders = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalNewUsers = 0;

        for (Map<String, Object> store : stores) {
            Long tenantId = (Long) store.get("tenantId");
            Map<String, Object> storeSummary = getTodaySummary(tenantId);
            totalOrders += ((Number) storeSummary.getOrDefault("totalOrders", 0)).intValue();
            totalAmount = totalAmount.add((BigDecimal) storeSummary.getOrDefault("todayAmount", BigDecimal.ZERO));
            totalNewUsers += ((Number) storeSummary.getOrDefault("newUsers", 0)).intValue();
        }

        dashboard.put("todayTotalOrders", totalOrders);
        dashboard.put("todayTotalAmount", totalAmount);
        dashboard.put("todayNewUsers", totalNewUsers);

        if (totalOrders > 0) {
            dashboard.put("avgOrderAmount",
                    totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
        }

        // 门店排行
        stores.sort((a, b) -> {
            int ordersA = ((Number) a.getOrDefault("todayOrders", 0)).intValue();
            int ordersB = ((Number) b.getOrDefault("todayOrders", 0)).intValue();
            return Integer.compare(ordersB, ordersA);
        });
        dashboard.put("storeRanking", stores);

        return dashboard;
    }

    private StoreDailySummary getTodaySummaryObj(Long tenantId) {
        LambdaQueryWrapper<StoreDailySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreDailySummary::getTenantId, tenantId)
               .eq(StoreDailySummary::getSummaryDate, LocalDate.now());
        return summaryMapper.selectOne(wrapper);
    }
}
