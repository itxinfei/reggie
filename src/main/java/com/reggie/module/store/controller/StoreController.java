package com.reggie.module.store.controller;

import com.reggie.common.R;
import com.reggie.entity.Tenant;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.model.StoreSearchDTO;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.store.service.StoreSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 门店管理控制器
 * 提供门店CRUD、数据隔离、商品同步等接口
 * 修改点：新增分页搜索、详情、编辑、批量操作、导出接口
 *
 * @author reggie
 * @since 2026-07-09
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
     * 分页搜索门店列表（支持多条件筛选与排序）
     * POST /store/page
     */
    @PostMapping("/page")
    public R<Map<String, Object>> pageStores(@RequestBody StoreSearchDTO dto) {
        Map<String, Object> result = storeService.searchStores(dto);
        return R.success(result);
    }

    /**
     * 获取所有门店列表（总部视角，兼容旧接口）
     * GET /store/list
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> listStores() {
        List<Map<String, Object>> stores = storeService.listAllStores();
        return R.success(stores);
    }

    /**
     * 获取门店详情（编辑回显用）
     * GET /store/detail/{tenantId}
     */
    @GetMapping("/detail/{tenantId}")
    public R<Map<String, Object>> getStoreDetail(@PathVariable Long tenantId) {
        Map<String, Object> detail = storeService.getStoreDetail(tenantId);
        return R.success(detail);
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
     * 编辑门店信息
     * PUT /store/update/{tenantId}
     */
    @PutMapping("/update/{tenantId}")
    public R<String> updateStore(@PathVariable Long tenantId,
                                  @RequestBody Map<String, Object> updateData) {
        storeService.updateStore(tenantId, updateData);
        return R.success("编辑成功");
    }

    /**
     * 切换门店
     * POST /store/switch/{tenantId}
     */
    @PostMapping("/switch/{tenantId}")
    public R<Map<String, Object>> switchStore(@PathVariable Long tenantId,
                                               HttpSession session) {
        Map<String, Object> storeInfo = storeService.switchStore(tenantId);
        session.setAttribute("tenantId", tenantId);
        log.info("[门店切换] Session tenantId已更新为: {}", tenantId);
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
     * 批量更新门店状态（上下架）
     * PUT /store/batch/status
     */
    @PutMapping("/batch/status")
    public R<Map<String, Object>> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> tenantIds = ((List<Integer>) body.get("tenantIds")).stream()
                .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        Integer status = Integer.valueOf(body.get("status").toString());
        int successCount = storeService.batchUpdateStoreStatus(tenantIds, status);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", successCount);
        result.put("total", tenantIds.size());
        return R.success(result);
    }

    /**
     * 导出门店数据为CSV
     * POST /store/export
     */
    @PostMapping("/export")
    public void exportStores(@RequestBody StoreSearchDTO dto, HttpServletResponse response) {
        List<Map<String, Object>> stores = storeService.exportStores(
                dto.getKeyword(), dto.getStoreType(), dto.getStatus());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "store_export_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        // BOM 头确保 Excel 正确识别 UTF-8
        response.setHeader("Cache-Control", "no-cache");

        try (OutputStream os = response.getOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            // BOM
            writer.write('\uFEFF');
            // CSV 头
            writer.write("门店名称,门店编码,门店类型,状态,联系人,联系电话,地址,营业时间," +
                    "外卖开关,堂食开关,配送半径(m),起送金额,配送费,今日订单,今日营收,创建时间\n");

            for (Map<String, Object> row : stores) {
                String storeName = nvl(row.get("storeName"));
                String storeCode = nvl(row.get("storeCode"));
                String storeType = storeTypeName(row.get("storeType"));
                String status = "1".equals(nvl(row.get("status"))) ? "启用" : "停用";
                String contactPerson = nvl(row.get("contactPerson"));
                String contactPhone = nvl(row.get("contactPhone"));
                String address = nvl(row.get("address"));
                String businessHours = nvl(row.get("businessHours"));
                String isDelivery = "1".equals(nvl(row.get("isDeliveryEnabled"))) ? "开" : "关";
                String isDineIn = "1".equals(nvl(row.get("isDineInEnabled"))) ? "开" : "关";
                String deliveryRadius = nvl(row.get("deliveryRadius"));
                String minAmount = nvl(row.get("minDeliveryAmount"));
                String deliveryFee = nvl(row.get("deliveryFee"));
                String todayOrders = nvl(row.get("todayOrders"));
                String todayAmount = nvl(row.get("todayAmount"));
                String createTime = row.get("createTime") != null ? row.get("createTime").toString() : "";

                writer.write(String.join(",", escapeCsv(storeName), escapeCsv(storeCode),
                        escapeCsv(storeType), escapeCsv(status), escapeCsv(contactPerson),
                        escapeCsv(contactPhone), escapeCsv(address), escapeCsv(businessHours),
                        isDelivery, isDineIn, deliveryRadius, minAmount, deliveryFee,
                        todayOrders, todayAmount, createTime) + "\n");
            }
            writer.flush();
            log.info("[门店导出] 导出成功: {} 条记录", stores.size());
        } catch (IOException e) {
            log.error("[门店导出] 写入CSV失败: {}", e.getMessage());
        }
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

        @SuppressWarnings("unchecked") // JSON反序列化类型转换，Integer转Long由调用方保证
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

        @SuppressWarnings("unchecked") // JSON反序列化类型转换，Integer转Long由调用方保证
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

    // ==================== 工具方法 ====================

    /**
     * 空值转空字符串
     */
    private String nvl(Object val) {
        return val == null ? "" : val.toString();
    }

    /**
     * CSV 字段转义（处理逗号和引号）
     */
    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    /**
     * 门店类型编码转中文
     */
    private String storeTypeName(Object typeObj) {
        if (typeObj == null) return "";
        int type = Integer.parseInt(typeObj.toString());
        switch (type) {
            case 1: return "直营总店";
            case 2: return "直营分店";
            case 3: return "加盟店";
            default: return String.valueOf(type);
        }
    }
}
