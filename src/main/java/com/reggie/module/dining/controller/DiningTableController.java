package com.reggie.module.dining.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.ChangeTableStatusDTO;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.mapper.DiningTableMapper;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

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
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private QRCodeUtil qrCodeUtil;

    @Autowired
    private DiningTableMapper diningTableMapper;

    @Autowired
    private com.reggie.module.dining.service.TableAreaService tableAreaService;

    /**
     * 分页查询桌台列表
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果（自动关联区域信息）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询桌台列表，自动关联区域信息")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<DiningTable>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        Page<DiningTable> pageInfo = diningTableService.pageWithArea(page, PageUtils.cap(pageSize));
        return R.success(pageInfo);
    }

    /**
     * 新增桌台
     * @param table 桌台信息
     * @return 新增桌台信息
     */
    @PostMapping
    @Operation(summary = "新增桌台", description = "创建新的桌台并关联区域")
    public R<DiningTable> save(@RequestBody DiningTable table) {
        log.info("新增桌台: {}", table.getName());
        table.setTenantId(BaseContext.getCurrentTenantId());
        diningTableService.save(table);
        return R.success(table);
    }

    /**
     * 修改桌台
     * @param table 桌台信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改桌台", description = "更新桌台基本信息")
    public R<String> update(@RequestBody DiningTable table) {
        log.info("修改桌台: {}", table.getId());
        // 租户归属校验：按 id + tenantId 查询，确认桌台归属当前租户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("无操作权限");
        }
        LambdaQueryWrapper<DiningTable> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DiningTable::getId, table.getId())
                    .eq(DiningTable::getTenantId, tenantId);
        if (diningTableService.count(checkWrapper) == 0) {
            return R.error("桌台不存在或无权操作");
        }
        // 防止通过请求体篡改租户ID
        table.setTenantId(tenantId);
        diningTableService.updateById(table);
        return R.success("修改桌台成功");
    }

    @DeleteMapping("/{id}")
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
        List<Map<String, Object>> rows = diningTableMapper.statByArea();
        int totalTables = 0;
        String maxArea = "-";
        int maxCount = 0;
        for (Map<String, Object> row : rows) {
            int cnt = row.get("cnt") == null ? 0 : ((Number) row.get("cnt")).intValue();
            totalTables += cnt;
            if (cnt > maxCount) {
                maxCount = cnt;
                Object areaName = row.get("areaName");
                maxArea = (areaName == null ? "未知区域" : String.valueOf(areaName)) + "(" + cnt + "桌)";
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalTables", totalTables);
        result.put("maxTablesArea", maxArea);
        return R.success(result);
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
            return R.error("生成二维码失败: " + e.getMessage());
        }
    }
}

