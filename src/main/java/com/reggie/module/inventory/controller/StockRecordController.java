package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 出入库记录控制器
 * 提供食材的入库、出库、记录查询等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/stock-record")
@Tag(name = "出入库记录")
public class StockRecordController {

    @Autowired
    private StockRecordService stockRecordService;

    /**
     * 分页查询出入库记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param materialId 食材ID（可选）
     * @param type 记录类型（可选）：IN-入库，OUT-出库，CHECK-盘点
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询出入库记录，支持按食材ID、类型和日期范围筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "materialId", description = "食材ID（可选）")
    @Parameter(name = "type", description = "类型（可选）：IN-入库，OUT-出库，CHECK-盘点")
    @Parameter(name = "startDate", description = "开始日期（可选）")
    @Parameter(name = "endDate", description = "结束日期（可选）")
    public R<Page<StockRecord>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) Long materialId,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate) {
        // 修改点：统一使用 LambdaQueryWrapper 支持所有筛选条件，而非分流到 pageByMaterial
        Page<StockRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
        qw.eq(materialId != null, StockRecord::getMaterialId, materialId);
        // 修改点：添加类型筛选支持，修复前端 type 参数被后端静默丢弃的 Bug
        qw.eq(type != null && !type.isEmpty(), StockRecord::getType, type);
        // 修改点：添加日期范围筛选支持，修复前端 dateRange 参数被后端静默丢弃的 Bug
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            qw.ge(StockRecord::getCreatedTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            qw.le(StockRecord::getCreatedTime, end);
        }
        qw.orderByDesc(StockRecord::getCreatedTime);
        stockRecordService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 食材入库
     * @param dto 入库请求
     * @return 操作结果
     */
    @PostMapping("/stockIn")
    @Operation(summary = "入库", description = "食材入库操作，增加库存数量")
    public R<String> stockIn(@Validated @RequestBody StockInDTO dto) {
        stockRecordService.stockIn(dto.getMaterialId(), dto.getQty(), dto.getUnitPrice(),
            dto.getBizId(), dto.getRemark(), dto.getOperator());
        return R.success("入库成功");
    }

    /**
     * 食材出库
     * @param dto 出库请求
     * @return 操作结果
     */
    @PostMapping("/stockOut")
    @Operation(summary = "出库", description = "食材出库操作，减少库存数量")
    public R<String> stockOut(@Validated @RequestBody StockOutDTO dto) {
        stockRecordService.stockOut(dto.getMaterialId(), dto.getQty(), dto.getBizId(),
            dto.getRemark(), dto.getOperator());
        return R.success("出库成功");
    }
}

