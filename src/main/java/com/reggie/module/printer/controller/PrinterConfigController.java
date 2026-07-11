package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 打印机配置管理控制器
 * 提供打印机配置的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/printer/config")
@Tag(name = "打印机配置")
public class PrinterConfigController {

    @Autowired
    private PrinterConfigService printerConfigService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询打印机配置列表，自动过滤当前租户数据，支持按名称、品牌、连接类型、状态筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "打印机名称（可选，模糊查询）")
    @Parameter(name = "brand", description = "品牌型号（可选，模糊查询）")
    @Parameter(name = "type", description = "连接类型（可选）：USB, TCP, CLOUD, BLUETOOTH")
    @Parameter(name = "status", description = "状态（可选）：1=启用, 0=停用")
    public R<Page<PrinterConfig>> page(int page, int pageSize, String name,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        Page<PrinterConfig> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null && !name.isEmpty(), PrinterConfig::getName, name);
        // 修改点：支持按品牌模糊查询
        queryWrapper.like(brand != null && !brand.isEmpty(), PrinterConfig::getBrand, brand);
        // 修改点：支持按连接类型筛选
        queryWrapper.eq(type != null && !type.isEmpty(), PrinterConfig::getType, type);
        // 修改点：支持按状态筛选
        queryWrapper.eq(status != null, PrinterConfig::getStatus, status);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(PrinterConfig::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(PrinterConfig::getSort);
        printerConfigService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增配置", description = "创建新的打印机配置")
    public R<String> save(@RequestBody PrinterConfig printerConfig) {
        log.info("新增打印机配置: {}", printerConfig.getName());
        printerConfig.setCreatedTime(LocalDateTime.now());
        printerConfig.setUpdatedTime(LocalDateTime.now());
        printerConfigService.save(printerConfig);
        return R.success("新增打印机配置成功");
    }

    @PutMapping
    @Operation(summary = "修改配置", description = "更新打印机配置信息")
    public R<String> update(@RequestBody PrinterConfig printerConfig) {
        log.info("修改打印机配置: {}", printerConfig.getId());
        printerConfig.setUpdatedTime(LocalDateTime.now());
        printerConfigService.updateById(printerConfig);
        return R.success("修改打印机配置成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "根据ID删除打印机配置")
    @Parameter(name = "id", description = "配置ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除打印机配置: {}", id);
        printerConfigService.removeById(id);
        return R.success("删除打印机配置成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询配置", description = "根据ID查询打印机配置详情")
    @Parameter(name = "id", description = "配置ID", required = true)
    public R<PrinterConfig> getById(@PathVariable Long id) {
        PrinterConfig config = printerConfigService.getById(id);
        if (config != null) {
            return R.success(config);
        }
        return R.error("没有查询到对应打印机配置");
    }

    @GetMapping("/list")
    @Operation(summary = "列表查询", description = "查询打印机配置列表，支持按打印类型筛选")
    @Parameter(name = "printType", description = "打印类型（可选）：BILL-小票、KITCHEN-厨房单、DELIVERY-配送单")
    public R<List<PrinterConfig>> list(@RequestParam(required = false) String printType) {
        LambdaQueryWrapper<PrinterConfig> queryWrapper = new LambdaQueryWrapper<>();
        // printTypes 字段存储逗号分隔值，使用 FIND_IN_SET 匹配
        if (printType != null && !printType.isEmpty()) {
            queryWrapper.apply("FIND_IN_SET({0}, print_types)", printType);
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(PrinterConfig::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(PrinterConfig::getSort);
        List<PrinterConfig> list = printerConfigService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 获取筛选下拉选项（名称列表 + 品牌列表）
     * <p>从数据库动态查询当前租户的所有打印机名称和品牌，供前端下拉框使用</p>
     *
     * @return 包含 nameOptions 和 brandOptions 的 Map
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取打印机名称和品牌列表，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<PrinterConfig> queryWrapper = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(PrinterConfig::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(PrinterConfig::getSort);
        List<PrinterConfig> list = printerConfigService.list(queryWrapper);

        Set<String> nameSet = new HashSet<>();
        Set<String> brandSet = new HashSet<>();
        for (PrinterConfig config : list) {
            if (config.getName() != null && !config.getName().isEmpty()) {
                nameSet.add(config.getName());
            }
            if (config.getBrand() != null && !config.getBrand().isEmpty()) {
                brandSet.add(config.getBrand());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        result.put("brands", new ArrayList<>(brandSet));
        return R.success(result);
    }
}

