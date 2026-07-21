package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 盘点管理控制器
 * 提供库存盘点单的创建、完成等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/stock-check")
@Tag(name = "盘点管理")
public class StockCheckController {

    @Autowired
    private StockCheckService stockCheckService;

    /**
     * 分页查询库存盘点记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 盘点单状态（可选）：DRAFT/IN_PROGRESS/DONE
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询库存盘点记录，支持按状态和日期范围筛选，按创建时间降序排列")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "status", description = "状态（可选）：DRAFT-草稿，IN_PROGRESS-进行中，DONE-已完成")
    @Parameter(name = "startDate", description = "开始日期（可选）")
    @Parameter(name = "endDate", description = "结束日期（可选）")
    public R<Page<StockCheck>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        Page<StockCheck> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
        qw.eq(status != null && !status.isEmpty(), StockCheck::getStatus, status);
        // 修改点：添加日期范围筛选支持，修复前端 dateRange 参数被后端静默丢弃的 Bug
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            qw.ge(StockCheck::getCreatedTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            qw.le(StockCheck::getCreatedTime, end);
        }
        qw.orderByDesc(StockCheck::getCreatedTime);
        stockCheckService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 创建库存盘点单
     * @param dto 盘点单创建请求
     * @return 盘点单信息
     */
    @PostMapping
    @Operation(summary = "创建盘点单", description = "创建新的库存盘点单")
    public R<StockCheck> create(@Validated @RequestBody CreateStockCheckDTO dto) {
        StockCheck sc = stockCheckService.createCheck(dto.getOperator(), dto.getRemark());
        return R.success(sc);
    }

    /**
     * 完成盘点并更新库存
     * @param id 盘点单ID
     * @param dto 盘点结果
     * @return 操作结果
     */
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成盘点", description = "提交盘点结果并更新库存")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> complete(@PathVariable Long id, @Valid @RequestBody CompleteStockCheckDTO dto) {
        stockCheckService.completeCheck(id, dto.getItems());
        return R.success("盘点完成");
    }
}

