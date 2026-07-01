package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.StockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inventory/stock-record")
@Tag(name = "出入库记录")
public class StockRecordController {

    @Autowired
    private StockRecordService stockRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<StockRecord>> page(int page, int pageSize, Long materialId) {
        if (materialId != null) {
            return R.success(stockRecordService.pageByMaterial(materialId, page, pageSize));
        }
        Page<StockRecord> pageInfo = new Page<>(page, pageSize);
        stockRecordService.page(pageInfo);
        return R.success(pageInfo);
    }

    @PostMapping("/stockIn")
    @Operation(summary = "入库")
    public R<String> stockIn(@RequestBody Map<String, Object> params) {
        Long materialId = Long.valueOf(params.get("materialId").toString());
        BigDecimal qty = new BigDecimal(params.get("qty").toString());
        BigDecimal unitPrice = params.get("unitPrice") != null ? new BigDecimal(params.get("unitPrice").toString()) : null;
        Long bizId = params.get("bizId") != null ? Long.valueOf(params.get("bizId").toString()) : null;
        String remark = (String) params.get("remark");
        String operator = (String) params.get("operator");
        stockRecordService.stockIn(materialId, qty, unitPrice, bizId, remark, operator);
        return R.success("入库成功");
    }

    @PostMapping("/stockOut")
    @Operation(summary = "出库")
    public R<String> stockOut(@RequestBody Map<String, Object> params) {
        Long materialId = Long.valueOf(params.get("materialId").toString());
        BigDecimal qty = new BigDecimal(params.get("qty").toString());
        Long bizId = params.get("bizId") != null ? Long.valueOf(params.get("bizId").toString()) : null;
        String remark = (String) params.get("remark");
        String operator = (String) params.get("operator");
        stockRecordService.stockOut(materialId, qty, bizId, remark, operator);
        return R.success("出库成功");
    }
}
