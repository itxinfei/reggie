package com.reggie.module.store.controller;
import com.reggie.common.utils.PageUtils;

import cn.hutool.core.util.StrUtil;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.store.dto.BatchUpdateStatusDTO;
import com.reggie.module.store.dto.CreateStoreDTO;
import com.reggie.module.store.dto.SyncCategoriesDTO;
import com.reggie.module.store.dto.SyncDishesDTO;
import com.reggie.module.store.dto.SyncSetmealsDTO;
import com.reggie.module.store.dto.UpdateStoreDTO;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.model.StoreSearchDTO;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.store.service.StoreSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
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
@RequiresAdmin
@RestController
@RequestMapping("/store")
@Tag(name = "门店管理", description = "门店CRUD、数据同步、商品管理及导出接口")
public class StoreController {

    @Autowired
    private StoreService storeService;
    @Autowired
    private StoreSyncService storeSyncService;

    // ==================== 门店管理 ====================

    /**
     * 分页搜索门店列表
     * @param dto 搜索条件（关键字、门店类型、状态等）
     * @return 分页结果
     */
    @PostMapping("/page")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "分页搜索门店", description = "分页搜索门店列表，支持多条件筛选与排序")
    @Parameter(name = "dto", description = "搜索条件（关键字、门店类型、状态等）", required = true)
    public R<Map<String, Object>> pageStores(@Valid @RequestBody StoreSearchDTO dto) {
        Map<String, Object> result = storeService.searchStores(dto);
        return R.success(result);
    }

    /**
     * 获取所有门店列表（总部视角）
     * @return 门店列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有门店列表", description = "总部视角获取所有门店列表，兼容旧接口")
    public R<List<Map<String, Object>>> listStores() {
        List<Map<String, Object>> stores = storeService.listAllStores();
        return R.success(stores);
    }

    /**
     * 获取门店详情
     * @param tenantId 租户ID
     * @return 门店详情
     */
    @GetMapping("/detail/{tenantId}")
    @Operation(summary = "获取门店详情", description = "根据tenantId获取门店详情，用于编辑回显")
    @Parameter(name = "tenantId", description = "租户ID", required = true)
    public R<Map<String, Object>> getStoreDetail(@PathVariable Long tenantId) {
        Map<String, Object> detail = storeService.getStoreDetail(tenantId);
        return R.success(detail);
    }

    /**
     * 获取某总店下的分店列表
     * GET /store/branches?parentTenantId=1
     */
    @GetMapping("/branches")
    @Operation(summary = "获取分店列表", description = "获取某总店下的分店列表")
    @Parameter(name = "parentTenantId", description = "总店tenantId", required = false)
    public R<List<StoreInfo>> listBranches(@RequestParam(required = false) Long parentTenantId) {
        List<StoreInfo> branches = storeService.listBranchStores(parentTenantId);
        return R.success(branches);
    }

    /**
     * 创建新门店
     * @param body 门店信息
     * @return 门店信息
     */
    @PostMapping("/create")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "创建门店", description = "创建新门店并关联租户和管理员账号")
    @Parameter(name = "body", description = "门店信息（包含门店名称、编码、类型、联系方式、管理员账号等）", required = true)
    public R<StoreInfo> createStore(@Valid @RequestBody CreateStoreDTO dto) {
        // 构建StoreInfo
        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setStoreCode(dto.getStoreCode());
        storeInfo.setStoreType(dto.getStoreType());
        storeInfo.setBusinessHours(dto.getBusinessHours());
        storeInfo.setDeliveryRadius(dto.getDeliveryRadius());
        storeInfo.setContactPerson(dto.getContactPerson());
        storeInfo.setContactPhone(dto.getContactPhone());

        Tenant tenant = new Tenant();
        tenant.setName(dto.getStoreName());
        tenant.setPhone(dto.getContactPhone());
        tenant.setAddress(dto.getAddress());
        tenant.setStatus(1);

        String username = dto.getAdminUsername();
        String password = dto.getAdminPassword();

        log.info("[门店管理] 创建门店请求: storeCode={}, contactPhone={}",
                dto.getStoreCode(), LogMaskUtils.maskPhone(dto.getContactPhone()));

        StoreInfo result = storeService.createStore(storeInfo, tenant, username, password);
        return R.success(result);
    }

    /**
     * 编辑门店信息
     * @param tenantId 租户ID
     * @param updateData 门店更新数据
     * @return 操作结果
     */
    @PutMapping("/update/{tenantId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "编辑门店", description = "根据tenantId更新门店信息")
    @Parameter(name = "tenantId", description = "租户ID", required = true)
    @Parameter(name = "updateData", description = "门店更新数据", required = true)
    public R<String> updateStore(@PathVariable Long tenantId,
                                  @Valid @RequestBody UpdateStoreDTO updateData) {
        storeService.updateStore(tenantId, updateData);
        return R.success("编辑成功");
    }

    /**
     * 切换门店
     * @param tenantId 目标租户ID
     * @param session HTTP会话
     * @return 门店信息
     */
    @PostMapping("/switch/{tenantId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "切换门店", description = "切换当前会话的租户门店")
    @Parameter(name = "tenantId", description = "目标租户ID", required = true)
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
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新门店状态", description = "根据tenantId更新门店启用/停用状态")
    @Parameter(name = "tenantId", description = "租户ID", required = true)
    @Parameter(name = "status", description = "状态值（1=启用 0=停用）", required = true)
    public R<String> updateStatus(@PathVariable Long tenantId,
                                   @Min(0) @Max(1) @RequestParam Integer status) {
        storeService.updateStoreStatus(tenantId, status);
        return R.success("状态更新成功");
    }

    /**
     * 批量更新门店状态
     * @param body 批量操作数据
     * @return 操作结果
     */
    @PutMapping("/batch/status")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量更新门店状态", description = "批量更新门店启用/停用状态（上下架）")
    @Parameter(name = "body", description = "批量操作数据（tenantIds状态列表、目标状态）", required = true)
    public R<Map<String, Object>> batchUpdateStatus(@Valid @RequestBody BatchUpdateStatusDTO dto) {
        List<Long> tenantIds = dto.getTenantIds();
        Integer status = dto.getStatus();
        int successCount = storeService.batchUpdateStoreStatus(tenantIds, status);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", successCount);
        result.put("total", tenantIds.size());
        return R.success(result);
    }

    /**
     * 导出门店数据为CSV
     * @param dto 搜索条件
     * @param response HTTP响应
     */
    @PostMapping("/export")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "导出门店数据", description = "导出门店数据为CSV文件")
    @Parameter(name = "dto", description = "搜索条件（关键字、门店类型、状态等）", required = true)
    public void exportStores(@Valid @RequestBody StoreSearchDTO dto, HttpServletResponse response) {
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

                writer.write(StrUtil.join(",", escapeCsv(storeName), escapeCsv(storeCode),
                        escapeCsv(storeType), escapeCsv(status), escapeCsv(contactPerson),
                        escapeCsv(contactPhone), escapeCsv(address), escapeCsv(businessHours),
                        isDelivery, isDineIn, deliveryRadius, minAmount, deliveryFee,
                        todayOrders, todayAmount, createTime) + "\n");
            }
            writer.flush();
            log.info("[门店导出] 导出成功: {} 条记录", stores.size());
        } catch (IOException e) {
            log.error("[门店导出] 写入CSV失败", e);
        }
    }

    /**
     * 门店统计（总部视角）
     * <p>使用 SQL 聚合替代前端 /store/list 拉全量后 filter 统计，消除 N+1 与全量内存计算</p>
     *
     * @return 门店总数、启用数、停用数、今日有营收门店数
     */
    @GetMapping("/stats")
    @Operation(summary = "门店统计", description = "聚合统计门店总数、启用数、停用数、今日有营收门店数")
    public R<Map<String, Object>> stats() {
        Map<String, Object> stats = storeService.getStoreStats();
        return R.success(stats);
    }

    /**
     * 获取门店今日概况
     * @param tenantId 租户ID（可选）
     * @return 门店今日经营概况
     */
    @GetMapping("/summary/today")
    @Operation(summary = "获取门店今日概况", description = "获取门店今日经营概况数据")
    @Parameter(name = "tenantId", description = "租户ID", required = false)
    public R<Map<String, Object>> todaySummary(@RequestParam(required = false) Long tenantId) {
        Map<String, Object> summary = storeService.getTodaySummary(tenantId);
        return R.success(summary);
    }

    // ==================== 数据同步 ====================

    /**
     * 同步菜品到目标门店
     * @param body 同步数据
     * @return 同步结果
     */
    @PostMapping("/sync/dishes")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "同步菜品", description = "将指定菜品从源门店同步到目标门店")
    @Parameter(name = "body", description = "同步数据（sourceTenantId, targetTenantId, dishIds, operatorId）", required = true)
    public R<Map<String, Object>> syncDishes(@Valid @RequestBody SyncDishesDTO dto) {
        Map<String, Object> result = storeSyncService.syncDishes(
                dto.getSourceTenantId(), dto.getTargetTenantId(), dto.getDishIds(), dto.getOperatorId());
        return R.success(result);
    }

    /**
     * 同步分类到目标门店
     * @param body 同步数据
     * @return 同步结果
     */
    @PostMapping("/sync/categories")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "同步分类", description = "将分类从源门店同步到目标门店")
    @Parameter(name = "body", description = "同步数据（sourceTenantId, targetTenantId, operatorId）", required = true)
    public R<Map<String, Object>> syncCategories(@Valid @RequestBody SyncCategoriesDTO dto) {
        Map<String, Object> result = storeSyncService.syncCategories(
                dto.getSourceTenantId(), dto.getTargetTenantId(), dto.getOperatorId());
        return R.success(result);
    }

    /**
     * 同步套餐到目标门店
     * @param body 同步数据
     * @return 同步结果
     */
    @PostMapping("/sync/setmeals")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "同步套餐", description = "将套餐从源门店同步到目标门店")
    @Parameter(name = "body", description = "同步数据（sourceTenantId, targetTenantId, setmealIds, operatorId）", required = true)
    public R<Map<String, Object>> syncSetmeals(@Valid @RequestBody SyncSetmealsDTO dto) {
        Map<String, Object> result = storeSyncService.syncSetmeals(
                dto.getSourceTenantId(), dto.getTargetTenantId(), dto.getSetmealIds(), dto.getOperatorId());
        return R.success(result);
    }

    /**
     * 查询菜品/分类/套餐同步操作日志
     * @param sourceTenantId 源门店tenantId
     * @param page 页码
     * @param pageSize 每页数量
     * @return 同步日志列表
     */
    @GetMapping("/sync/logs")
    @Operation(summary = "查询同步日志", description = "查询菜品/分类/套餐同步操作日志")
    @Parameter(name = "sourceTenantId", description = "源门店tenantId", required = true)
    @Parameter(name = "page", description = "页码", required = false, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = false, example = "10")
    public R<List<Map<String, Object>>> syncLogs(
            @RequestParam Long sourceTenantId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "PageSize")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        List<Map<String, Object>> logs = storeSyncService.getSyncLogs(sourceTenantId, page, PageUtils.cap(pageSize));
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




