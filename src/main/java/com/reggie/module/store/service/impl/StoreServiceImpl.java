package com.reggie.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.PasswordUtils;
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
import java.util.*;

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

    // 修改点：新增编辑方法
    @Override
    @Transactional
    public void updateStore(Long tenantId, Map<String, Object> updateData) {
        // 1. 更新 Tenant 表
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            log.warn("[门店管理] 编辑门店失败: tenantId={} 不存在", tenantId);
            return;
        }
        if (updateData.containsKey("storeName")) {
            tenant.setName((String) updateData.get("storeName"));
        }
        if (updateData.containsKey("address")) {
            tenant.setAddress((String) updateData.get("address"));
        }
        if (updateData.containsKey("status")) {
            tenant.setStatus(Integer.valueOf(updateData.get("status").toString()));
        }
        tenantService.updateById(tenant);

        // 2. 更新 StoreInfo 表
        StoreInfo storeInfo = storeInfoMapper.findByTenantId(tenantId);
        if (storeInfo != null) {
            if (updateData.containsKey("storeCode")) {
                storeInfo.setStoreCode((String) updateData.get("storeCode"));
            }
            if (updateData.containsKey("storeType")) {
                storeInfo.setStoreType(Integer.valueOf(updateData.get("storeType").toString()));
            }
            if (updateData.containsKey("businessHours")) {
                storeInfo.setBusinessHours((String) updateData.get("businessHours"));
            }
            if (updateData.containsKey("contactPerson")) {
                storeInfo.setContactPerson((String) updateData.get("contactPerson"));
            }
            if (updateData.containsKey("contactPhone")) {
                storeInfo.setContactPhone((String) updateData.get("contactPhone"));
            }
            if (updateData.containsKey("deliveryRadius")) {
                storeInfo.setDeliveryRadius(Integer.valueOf(updateData.get("deliveryRadius").toString()));
            }
            if (updateData.containsKey("minDeliveryAmount")) {
                storeInfo.setMinDeliveryAmount(new BigDecimal(updateData.get("minDeliveryAmount").toString()));
            }
            if (updateData.containsKey("deliveryFee")) {
                storeInfo.setDeliveryFee(new BigDecimal(updateData.get("deliveryFee").toString()));
            }
            if (updateData.containsKey("isDeliveryEnabled")) {
                storeInfo.setIsDeliveryEnabled(Integer.valueOf(updateData.get("isDeliveryEnabled").toString()));
            }
            if (updateData.containsKey("isDineInEnabled")) {
                storeInfo.setIsDineInEnabled(Integer.valueOf(updateData.get("isDineInEnabled").toString()));
            }
            storeInfoMapper.updateById(storeInfo);
        }

        log.info("[门店管理] 编辑门店成功: tenantId={}", tenantId);
    }

    @Override
    public List<Map<String, Object>> listAllStores() {
        List<StoreInfo> stores = storeInfoMapper.selectList(null);

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
            map.put("contactPerson", si.getContactPerson());

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

    // 修改点：新增分页搜索方法
    @Override
    public Map<String, Object> searchStores(StoreSearchDTO dto) {
        Page<Map<String, Object>> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 防止 SQL 注入：sortOrder 仅允许 asc/desc
        String sortOrder = "desc";
        if (dto.getSortOrder() != null && "asc".equalsIgnoreCase(dto.getSortOrder())) {
            sortOrder = "asc";
        }
        IPage<Map<String, Object>> result = storeInfoMapper.searchStores(
                page, dto.getKeyword(), dto.getStoreType(), dto.getStatus(),
                dto.getSortBy(), sortOrder);

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

        // 今日新增用户（按租户隔离）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getTenantId, tenantId)
                .ge(User::getCreateTime, today.atStartOfDay())
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

    // 修改点：新增批量更新状态方法
    @Override
    @Transactional
    public int batchUpdateStoreStatus(List<Long> tenantIds, Integer status) {
        int successCount = 0;
        for (Long tenantId : tenantIds) {
            try {
                Tenant tenant = tenantService.getById(tenantId);
                if (tenant != null) {
                    tenant.setStatus(status);
                    tenantService.updateById(tenant);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("[门店管理] 批量更新状态失败: tenantId={}, error={}", tenantId, e.getMessage());
            }
        }
        log.info("[门店管理] 批量更新门店状态完成: 成功{}个, 目标状态={}", successCount, status);
        return successCount;
    }

    // 修改点：新增导出方法
    @Override
    public List<Map<String, Object>> exportStores(String keyword, Integer storeType, Integer status) {
        return storeInfoMapper.exportStores(keyword, storeType, status);
    }

    @Override
    public Map<String, Object> getAggregatedDashboard() {
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
