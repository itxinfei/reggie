package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.ChangeTableStatusDTO;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.util.QRCodeUtil;
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

@Slf4j
@RestController
@RequestMapping("/api/dining/table")
@Tag(name = "堂食桌台管理")
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private QRCodeUtil qrCodeUtil;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询桌台列表，自动关联区域信息")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<DiningTable>> page(int page, int pageSize) {
        Page<DiningTable> pageInfo = diningTableService.pageWithArea(page, pageSize);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增桌台", description = "创建新的桌台并关联区域")
    public R<DiningTable> save(@RequestBody DiningTable table) {
        log.info("新增桌台: {}", table.getName());
        table.setTenantId(BaseContext.getCurrentTenantId());
        diningTableService.save(table);
        return R.success(table);
    }

    @PutMapping
    @Operation(summary = "修改桌台", description = "更新桌台基本信息")
    public R<String> update(@RequestBody DiningTable table) {
        log.info("修改桌台: {}", table.getId());
        diningTableService.updateById(table);
        return R.success("修改桌台成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除桌台", description = "根据ID删除桌台")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除桌台: {}", id);
        diningTableService.removeById(id);
        return R.success("删除桌台成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询桌台", description = "根据ID查询桌台详情")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<DiningTable> getById(@PathVariable Long id) {
        DiningTable table = diningTableService.getById(id);
        if (table != null) {
            return R.success(table);
        }
        return R.error("没有查询到对应桌台");
    }

    @PutMapping("/status")
    @Operation(summary = "修改桌台状态", description = "更新桌台使用状态（空闲/使用中/已预订等）")
    public R<String> changeStatus(@Valid @RequestBody ChangeTableStatusDTO dto) {
        log.info("修改桌台状态: id={}, status={}", dto.getId(), dto.getStatus());
        diningTableService.changeStatus(dto.getId(), dto.getStatus());
        return R.success("修改状态成功");
    }

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

