package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.TableAreaService;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dining/area")
@Tag(name = "堂食区域管理")
public class TableAreaController {

    @Autowired
    private TableAreaService tableAreaService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询桌台区域列表")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<TableArea>> page(int page, int pageSize) {
        Page<TableArea> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<TableArea> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TableArea::getSort);
        tableAreaService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增区域", description = "创建新的桌台区域")
    public R<TableArea> save(@RequestBody TableArea area) {
        log.info("新增区域: {}", area.getName());
        area.setTenantId(BaseContext.getCurrentTenantId());
        tableAreaService.save(area);
        return R.success(area);
    }

    @PutMapping
    @Operation(summary = "修改区域", description = "更新桌台区域信息")
    public R<String> update(@RequestBody TableArea area) {
        log.info("修改区域: {}", area.getId());
        tableAreaService.updateById(area);
        return R.success("修改区域成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除区域", description = "根据ID删除桌台区域")
    @Parameter(name = "id", description = "区域ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除区域: {}", id);
        tableAreaService.removeById(id);
        return R.success("删除区域成功");
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有区域", description = "查询所有桌台区域列表")
    public R<List<TableArea>> list() {
        LambdaQueryWrapper<TableArea> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TableArea::getSort);
        List<TableArea> list = tableAreaService.list(qw);
        return R.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询区域", description = "根据ID查询桌台区域详情")
    @Parameter(name = "id", description = "区域ID", required = true)
    public R<TableArea> getById(@PathVariable Long id) {
        TableArea area = tableAreaService.getById(id);
        if (area != null) {
            return R.success(area);
        }
        return R.error("没有查询到对应区域");
    }
}

