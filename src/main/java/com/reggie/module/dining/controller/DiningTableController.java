package com.reggie.module.dining.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.ChangeTableStatusDTO;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.vo.TableStatsVO;
import com.reggie.utils.QRCodeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 堂食桌台管理控制器
 * 提供桌台的增删改查、状态管理、二维码生成等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/dining/table")
@Tag(name = "堂食桌台管理")
@RequireEmployee
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private QRCodeUtil qrCodeUtil;

    @Autowired
    private com.reggie.module.dining.service.TableAreaService tableAreaService;

    /**
     * 分页查询桌台列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 桌台名称（可选，模糊搜索）
     * @param areaId 区域ID（可选）
     * @param status 桌台状态（可选）
     * @return 分页结果（自动关联区域信息）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询桌台列表，支持按名称、区域、状态筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "桌台名称（可选，模糊搜索）")
    @Parameter(name = "areaId", description = "区域ID（可选）")
    @Parameter(name = "status", description = "桌台状态（可选）：FREE-空闲, OCCUPIED-占用, RESERVED-预留, CLEANING-清洁中")
    public R<Page<DiningTable>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) Long areaId,
                                     @RequestParam(required = false) String status) {
        Page<DiningTable> pageInfo = diningTableService.pageWithArea(page, PageUtils.cap(pageSize), name, areaId, status);
        return R.success(pageInfo);
    }

    /**
     * 桌台统计（按状态分类计数）
     *
     * @return 桌台统计
     */
    @GetMapping("/stats")
    @Operation(summary = "桌台统计", description = "按状态分类统计桌台数量")
    public R<TableStatsVO> tableStats() {
        TableStatsVO stats = diningTableService.tableStats();
        return R.success(stats);
    }

    /**
     * 新增桌台
     * @param table 桌台信息
     * @return 新增桌台信息
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增桌台", description = "创建新的桌台并关联区域")
    public R<DiningTable> save(@Valid @RequestBody DiningTable table) {
        log.info("新增桌台: {}", table.getName());
        table.setTenantId(BaseContext.getCurrentTenantId());
        if (table.getStatus() == null || table.getStatus().trim().isEmpty()) {
            table.setStatus("FREE");
        }
        diningTableService.save(table);
        return R.success(table);
    }

    /**
     * 修改桌台
     * @param table 桌台信息
     * @return 操作结果
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改桌台", description = "更新桌台基本信息")
    public R<String> update(@Valid @RequestBody DiningTable table) {
        log.info("修改桌台: {}", table.getId());
        // 租户归属校验：按 id + tenantId 查询，确认桌台归属当前租户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        LambdaQueryWrapper<DiningTable> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DiningTable::getId, table.getId())
                    .eq(DiningTable::getTenantId, tenantId);
        DiningTable existing = diningTableService.getOne(checkWrapper);
        if (existing == null) {
            return R.error("桌台不存在或无权操作");
        }
        // 请求体未传状态时，保留原状态（避免 @NotBlank 导致更新时必填）
        if (table.getStatus() == null || table.getStatus().trim().isEmpty()) {
            table.setStatus(existing.getStatus());
        }
        // 防止通过请求体篡改租户ID
        table.setTenantId(tenantId);
        diningTableService.updateById(table);
        return R.success("修改桌台成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除桌台", description = "根据ID删除桌台")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除桌台: {}", id);
        // 租户归属校验：按 id + tenantId 查询，确认桌台归属当前租户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        LambdaQueryWrapper<DiningTable> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DiningTable::getId, id)
                    .eq(DiningTable::getTenantId, tenantId);
        if (diningTableService.count(checkWrapper) == 0) {
            return R.error("桌台不存在或无权操作");
        }
        diningTableService.removeById(id);
        return R.success("删除桌台成功");
    }

    /**
     * 根据ID查询桌台
     * @param id 桌台ID
     * @return 桌台详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询桌台", description = "根据ID查询桌台详情")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<DiningTable> getById(@PathVariable Long id) {
        // 租户归属校验：按 id + tenantId 条件查询，防止跨租户 IDOR
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        qw.eq(DiningTable::getId, id)
          .eq(DiningTable::getTenantId, tenantId);
        DiningTable table = diningTableService.getOne(qw);
        if (table != null) {
            return R.success(table);
        }
        return R.error("没有查询到对应桌台");
    }

    /**
     * 修改桌台状态
     * @param dto 桌台状态变更请求
     * @return 操作结果
     */
    @PutMapping("/status")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改桌台状态", description = "更新桌台使用状态（空闲/使用中/已预订等）")
    public R<String> changeStatus(@Valid @RequestBody ChangeTableStatusDTO dto) {
        log.info("修改桌台状态: id={}, status={}", dto.getId(), dto.getStatus());
        diningTableService.changeStatus(dto.getId(), dto.getStatus());
        return R.success("修改状态成功");
    }

    /**
     * 桌台列表（不分页）
     */
    @GetMapping("/list")
    @Operation(summary = "桌台列表", description = "获取所有桌台列表")
    public R<List<DiningTable>> list() {
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        // 强制租户过滤，防止跨租户数据泄露
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        qw.eq(DiningTable::getTenantId, tenantId);
        qw.orderByAsc(DiningTable::getSort);
        List<DiningTable> list = diningTableService.list(qw);
        // 批量填充区域名称，避免 N+1 查询
        if (list != null && !list.isEmpty()) {
            java.util.Set<Long> areaIds = new java.util.HashSet<>();
            for (DiningTable table : list) {
                if (table.getAreaId() != null) {
                    areaIds.add(table.getAreaId());
                }
            }
            Map<Long, String> areaNameMap = new HashMap<>();
            if (!areaIds.isEmpty()) {
                for (TableArea area : tableAreaService.listByIds(areaIds)) {
                    areaNameMap.put(area.getId(), area.getName());
                }
            }
            for (DiningTable table : list) {
                if (table.getAreaId() != null) {
                    table.setAreaName(areaNameMap.get(table.getAreaId()));
                }
            }
        }
        return R.success(list);
    }

    /**
     * 桌台区域统计（区域管理页用）
     * <p>使用 SQL 按区域分组聚合替代前端 pageSize:999 拉全量后前端分组，避免全表扫描</p>
     *
     * @return 桌台总数、最大容量区域（名称+桌数）
     */
    @GetMapping("/area-stats")
    @Operation(summary = "桌台区域统计", description = "聚合统计桌台总数与最大容量区域")
    public R<Map<String, Object>> areaStats() {
        return R.success(diningTableService.areaStats());
    }

    /**
     * 生成桌台二维码
     * @param id 桌台ID
     * @return Base64格式二维码图片
     */
    @GetMapping("/qrcode/{id}")
    @Operation(summary = "生成桌台二维码", description = "生成桌台扫码点餐二维码（Base64格式）")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<String> qrcode(@PathVariable Long id) {
        DiningTable table = diningTableService.getById(id);
        if (table == null) {
            return R.error("桌台不存在");
        }

        try {
            // 生成二维码（Base64格式）
            String qrCodeBase64 = qrCodeUtil.generateTableQRCode(id, table.getName());

            // 返回Base64图片数据
            return R.success("data:image/png;base64," + qrCodeBase64);
        } catch (Exception e) {
            log.error("生成二维码失败: tableId={}", id, e);
            return R.error("生成二维码失败，请稍后重试");
        }
    }
}

