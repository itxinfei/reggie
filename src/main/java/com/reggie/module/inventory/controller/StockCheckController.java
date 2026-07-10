package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.CompleteStockCheckDTO;
import com.reggie.dto.CreateStockCheckDTO;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.service.StockCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 盘点管理控制器
 * 提供库存盘点单的创建、完成等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/stock-check")
@Tag(name = "盘点管理")
public class StockCheckController {

    @Autowired
    private StockCheckService stockCheckService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询库存盘点记录，按创建时间降序排列")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<StockCheck>> page(int page, int pageSize) {
        Page<StockCheck> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(StockCheck::getCreatedTime);
        stockCheckService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "创建盘点单", description = "创建新的库存盘点单")
    public R<StockCheck> create(@Validated @RequestBody CreateStockCheckDTO dto) {
        StockCheck sc = stockCheckService.createCheck(dto.getOperator(), dto.getRemark());
        return R.success(sc);
    }

    @PutMapping("/complete/{id}")
    @Operation(summary = "完成盘点", description = "提交盘点结果并更新库存")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> complete(@PathVariable Long id, @Valid @RequestBody CompleteStockCheckDTO dto) {
        stockCheckService.completeCheck(id, dto.getItems());
        return R.success("盘点完成");
    }
}

