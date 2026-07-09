package com.reggie.module.store.controller;

import com.reggie.common.R;
import com.reggie.entity.Tenant;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.store.service.StoreSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 门店管理Controller
 * 提供门店CRUD、数据隔离、商品同步等API
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private StoreService storeService;
    @Autowired
    private StoreSyncService storeSyncService;

    // ==================== 门店管理 ====================

    /**
     * 获取所有门店列表（总部视角）
     * GET /store/list
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> listStores() {
        List<Map<String, Object>> stores = storeService.listAllStores();
        return R.success(stores);
    }

    /**
     * 获取某总店下的分店列表
     * GET /store/branches?parentTenantId=1
     */
    @GetMapping("/branches")
    public R<List<StoreInfo>> listBranches(@RequestParam(required = false) Long parentTenantId) {
        List<StoreInfo> branches = storeService.listBranchStores(parentTenantId);
        return R.success(branches);
    }

    /**
     * 创建门店
     * POST /store/create
     */
    @PostMapping("/create")
    public R<StoreInfo> createStore(@Valid @RequestBody Map<String, Object> body) {
        // 构建StoreInfo
        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setStoreCode((String) body.get("storeCode"));
        storeInfo.setStoreType(body.get("storeType") != null ?
                Integer.valueOf(body.get("storeType").toString()) : StoreInfo.TYPE_DIRECT_BRANCH);
        storeInfo.setBusinessHours((String) body.get("businessHours"));
        storeInfo.setDeliveryRadius(body.get("deliveryRadius") != null ?
                Integer.valueOf(body.get("deliveryRadius").toString()) : 3000);
        storeInfo.setContactPerson((String) body.get("contactPerson"));
        storeInfo.setContactPhone((String) body.get("contactPhone"));

        // 构建Tenant
        Tenant tenant = new Tenant();
        tenant.setName((String) body.get("storeName"));
        tenant.setPhone((String) body.get("contactPhone"));
        tenant.setAddress((String) body.get("address"));
        tenant.setStatus(1);

        String username = (String) body.get("adminUsername");
        String password = (String) body.get("adminPassword");

        StoreInfo result = storeService.createStore(storeInfo, tenant, username, password);
        return R.success(result);
    }

    /**
     * 切换门店
     * POST /store/switch/{tenantId}
     */
    @PostMapping("/switch/{tenantId}")
    public R<Map<String, Object>> switchStore(@PathVariable Long tenantId) {
        Map<String, Object> storeInfo = storeService.switchStore(tenantId);
        return R.success(storeInfo);
    }

    /**
     * 更新门店状态
     * PUT /store/{tenantId}/status
     */
    @PutMapping("/{tenantId}/status")
    public R<String> updateStatus(@PathVariable Long tenantId,
                                   @RequestParam Integer status) {
        storeService.updateStoreStatus(tenantId, status);
        return R.success("状态更新成功");
    }

    /**
     * 获取门店今日概况
     * GET /store/summary/today?tenantId=1
     */
    @GetMapping("/summary/today")
    public R<Map<String, Object>> todaySummary(@RequestParam(required = false) Long tenantId) {
        Map<String, Object> summary = storeService.getTodaySummary(tenantId);
        return R.success(summary);
    }

    // ==================== 数据同步 ====================

    /**
     * 同步菜品到目标门店
     * POST /store/sync/dishes
     */
    @PostMapping("/sync/dishes")
    public R<Map<String, Object>> syncDishes(@RequestBody Map<String, Object> body) {
        Long sourceTenantId = Long.valueOf(body.get("sourceTenantId").toString());
        Long targetTenantId = Long.valueOf(body.get("targetTenantId").toString());
        Long operatorId = body.get("operatorId") != null ?
                Long.valueOf(body.get("operatorId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Long> dishIds = body.get("dishIds") != null ?
                ((List<Integer>) body.get("dishIds")).stream()
                        .map(Long::valueOf).collect(java.util.stream.Collectors.toList()) : null;

        Map<String, Object> result = storeSyncService.syncDishes(sourceTenantId, targetTenantId, dishIds, operatorId);
        return R.success(result);
    }

    /**
     * 同步分类到目标门店
     * POST /store/sync/categories
     */
    @PostMapping("/sync/categories")
    public R<Map<String, Object>> syncCategories(@RequestBody Map<String, Object> body) {
        Long sourceTenantId = Long.valueOf(body.get("sourceTenantId").toString());
        Long targetTenantId = Long.valueOf(body.get("targetTenantId").toString());
        Long operatorId = body.get("operatorId") != null ?
                Long.valueOf(body.get("operatorId").toString()) : null;

        Map<String, Object> result = storeSyncService.syncCategories(sourceTenantId, targetTenantId, operatorId);
        return R.success(result);
    }

    /**
     * 同步套餐到目标门店
     * POST /store/sync/setmeals
     */
    @PostMapping("/sync/setmeals")
    public R<Map<String, Object>> syncSetmeals(@RequestBody Map<String, Object> body) {
        Long sourceTenantId = Long.valueOf(body.get("sourceTenantId").toString());
        Long targetTenantId = Long.valueOf(body.get("targetTenantId").toString());
        Long operatorId = body.get("operatorId") != null ?
                Long.valueOf(body.get("operatorId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Long> setmealIds = body.get("setmealIds") != null ?
                ((List<Integer>) body.get("setmealIds")).stream()
                        .map(Long::valueOf).collect(java.util.stream.Collectors.toList()) : null;

        Map<String, Object> result = storeSyncService.syncSetmeals(sourceTenantId, targetTenantId, setmealIds, operatorId);
        return R.success(result);
    }

    /**
     * 查询同步日志
     * GET /store/sync/logs?sourceTenantId=1&page=1&pageSize=10
     */
    @GetMapping("/sync/logs")
    public R<List<Map<String, Object>>> syncLogs(
            @RequestParam Long sourceTenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<Map<String, Object>> logs = storeSyncService.getSyncLogs(sourceTenantId, page, pageSize);
        return R.success(logs);
    }
}
