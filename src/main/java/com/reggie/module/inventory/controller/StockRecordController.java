package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.StockInDTO;
import com.reggie.dto.StockOutDTO;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.StockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

/**
 * 出入库记录控制器
 * 提供食材的入库、出库、记录查询等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/stock-record")
@Tag(name = "出入库记录")
public class StockRecordController {

    @Autowired
    private StockRecordService stockRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询出入库记录，支持按食材ID筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "materialId", description = "食材ID（可选）")
    public R<Page<StockRecord>> page(int page, int pageSize, Long materialId) {
        if (materialId != null) {
            return R.success(stockRecordService.pageByMaterial(materialId, page, pageSize));
        }
        Page<StockRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(BaseContext.getCurrentTenantId() != null, StockRecord::getTenantId, BaseContext.getCurrentTenantId());
        qw.orderByDesc(StockRecord::getCreatedTime);
        stockRecordService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping("/stockIn")
    @Operation(summary = "入库", description = "食材入库操作，增加库存数量")
    public R<String> stockIn(@Validated @RequestBody StockInDTO dto) {
        stockRecordService.stockIn(dto.getMaterialId(), dto.getQty(), dto.getUnitPrice(),
            dto.getBizId(), dto.getRemark(), dto.getOperator());
        return R.success("入库成功");
    }

    @PostMapping("/stockOut")
    @Operation(summary = "出库", description = "食材出库操作，减少库存数量")
    public R<String> stockOut(@Validated @RequestBody StockOutDTO dto) {
        stockRecordService.stockOut(dto.getMaterialId(), dto.getQty(), dto.getBizId(),
            dto.getRemark(), dto.getOperator());
        return R.success("出库成功");
    }
}

