package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/printer/config")
@Tag(name = "打印机配置")
public class PrinterConfigController {

    @Autowired
    private PrinterConfigService printerConfigService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<PrinterConfig>> page(int page, int pageSize, String name) {
        Page<PrinterConfig> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null && !name.isEmpty(), PrinterConfig::getName, name);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(PrinterConfig::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(PrinterConfig::getSort);
        printerConfigService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增配置")
    public R<String> save(@RequestBody PrinterConfig printerConfig) {
        log.info("新增打印机配置: {}", printerConfig.getName());
        printerConfig.setCreatedTime(LocalDateTime.now());
        printerConfig.setUpdatedTime(LocalDateTime.now());
        printerConfigService.save(printerConfig);
        return R.success("新增打印机配置成功");
    }

    @PutMapping
    @Operation(summary = "修改配置")
    public R<String> update(@RequestBody PrinterConfig printerConfig) {
        log.info("修改打印机配置: {}", printerConfig.getId());
        printerConfig.setUpdatedTime(LocalDateTime.now());
        printerConfigService.updateById(printerConfig);
        return R.success("修改打印机配置成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    public R<String> delete(@PathVariable Long id) {
        log.info("删除打印机配置: {}", id);
        printerConfigService.removeById(id);
        return R.success("删除打印机配置成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询配置")
    public R<PrinterConfig> getById(@PathVariable Long id) {
        PrinterConfig config = printerConfigService.getById(id);
        if (config != null) {
            return R.success(config);
        }
        return R.error("没有查询到对应打印机配置");
    }

    @GetMapping("/list")
    @Operation(summary = "列表查询")
    public R<List<PrinterConfig>> list(@RequestParam(required = false) String printType) {
        LambdaQueryWrapper<PrinterConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(printType != null && !printType.isEmpty(), PrinterConfig::getPrintType, printType);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(PrinterConfig::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(PrinterConfig::getSort);
        List<PrinterConfig> list = printerConfigService.list(queryWrapper);
        return R.success(list);
    }
}
