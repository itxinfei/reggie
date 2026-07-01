package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dining/table")
@Tag(name = "堂食桌台管理")
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<DiningTable>> page(int page, int pageSize) {
        Page<DiningTable> pageInfo = diningTableService.pageWithArea(page, pageSize);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增桌台")
    public R<DiningTable> save(@RequestBody DiningTable table) {
        log.info("新增桌台: {}", table.getName());
        table.setTenantId(BaseContext.getCurrentTenantId());
        diningTableService.save(table);
        return R.success(table);
    }

    @PutMapping
    @Operation(summary = "修改桌台")
    public R<String> update(@RequestBody DiningTable table) {
        log.info("修改桌台: {}", table.getId());
        diningTableService.updateById(table);
        return R.success("修改桌台成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除桌台")
    public R<String> delete(@PathVariable Long id) {
        log.info("删除桌台: {}", id);
        diningTableService.removeById(id);
        return R.success("删除桌台成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询桌台")
    public R<DiningTable> getById(@PathVariable Long id) {
        DiningTable table = diningTableService.getById(id);
        if (table != null) {
            return R.success(table);
        }
        return R.error("没有查询到对应桌台");
    }

    @PutMapping("/status")
    @Operation(summary = "修改桌台状态")
    public R<String> changeStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String status = (String) params.get("status");
        log.info("修改桌台状态: id={}, status={}", id, status);
        diningTableService.changeStatus(id, status);
        return R.success("修改状态成功");
    }

    @GetMapping("/qrcode/{id}")
    @Operation(summary = "生成桌台二维码")
    public R<String> qrcode(@PathVariable Long id) {
        DiningTable table = diningTableService.getById(id);
        if (table == null) {
            return R.error("桌台不存在");
        }
        String placeholderUrl = "https://qr.dining.example.com/table/" + id;
        table.setQrCodeUrl(placeholderUrl);
        diningTableService.updateById(table);
        return R.success(placeholderUrl);
    }
}
