package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
// 修改点(2026-09-18)：原 import 路径 com.reggie.common.annotation.RateLimit /
// com.reggie.common.enums.RateLimitType 均不存在（全项目统一在 com.reggie.common 包），
// 会导致该控制器无法编译，进而阻断整个工程构建。
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.dto.CompleteStockCheckDTO;
import com.reggie.dto.CreateStockCheckDTO;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.enums.StockCheckStatus;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.service.StockCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.reggie.module.inventory.model.StockCheckDetail;

/**
 * 盘点管理控制器
 * 提供库存盘点单的创建、完成等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
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
    public R<Page<StockCheck>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue =
            "10") @Min(1) @Max(100) int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String startDate,
                                     @Parameter(description = "结束日期（可选），格式yyyy-MM-dd")
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
    public R<StockCheck> create(@Parameter(description = "盘点单创建信息", required =
            true) @Validated @RequestBody CreateStockCheckDTO dto) {
        StockCheck sc = stockCheckService.createCheck(dto.getOperator(), dto.getRemark(), dto.getVoucherImages());
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
    public R<String> complete(@Parameter(description = "盘点单ID", required = true) @PathVariable Long id,
            @Parameter(description = "盘点结果明细", required = true) @Valid @RequestBody CompleteStockCheckDTO dto) {
        stockCheckService.completeCheck(id, dto.getItems());
        return R.success("盘点完成");
    }

    /**
     * 获取盘点统计
     * @return 统计数据 {total, draft, inProgress, done, diffCount}
     */
    @GetMapping("/stats")
    @Operation(summary = "盘点统计", description = "获取盘点单状态统计，含总数/草稿/进行中/已完成/差异项数")
    public R<Map<String, Object>> stats() {
        return R.success(stockCheckService.getStats());
    }

    /**
     * 获取盘点单明细列表
     * @param id 盘点单ID
     * @return 明细列表
     */
    @GetMapping("/{id}/details")
    @Operation(summary = "盘点明细", description = "获取指定盘点单的明细列表，含食材名称和差异信息")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<List<StockCheckDetail>> details(@PathVariable Long id) {
        return R.success(stockCheckService.getDetails(id));
    }

    /**
     * 设置盘点项（添加食材 + 快照账面数量）
     * @param id 盘点单ID
     * @param items 食材列表
     * @return 操作结果
     */
    @PutMapping("/{id}/items")
    @Operation(summary = "设置盘点项", description = "为盘点单添加盘点食材，自动快照当前库存作为账面数量")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> setItems(@PathVariable Long id,
            @Valid @RequestBody List<StockCheckItemDTO> items) {
        stockCheckService.setCheckItems(id, items);
        return R.success("盘点项设置成功");
    }

    /**
     * 录入实际库存数量
     * @param id 盘点单ID
     * @param items [{materialId, actualStock}]
     * @return 操作结果
     */
    @PutMapping("/{id}/record")
    @Operation(summary = "录入实盘数量", description = "为盘点单中的食材录入实际库存数量")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> record(@PathVariable Long id,
            @Valid @RequestBody List<StockCheckItemDTO> items) {
        stockCheckService.recordActualQty(id, items);
        return R.success("实盘数量录入成功");
    }

    /**
     * 删除盘点单（逻辑删除）
     * <p>StockCheck 带 @TableLogic，removeById 为逻辑删除。
     * 仅草稿/进行中状态可删除；已完成的盘点单已生成库存差异并影响对账，禁止删除。</p>
     *
     * @param id 盘点单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除盘点单", description = "逻辑删除盘点单，仅草稿/进行中状态可删除")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        StockCheck existing = stockCheckService.getById(id);
        if (existing == null) {
            return R.error("盘点单不存在");
        }
        boolean canDelete = StockCheckStatus.DRAFT.getValue().equals(existing.getStatus())
                || StockCheckStatus.IN_PROGRESS.getValue().equals(existing.getStatus());
        if (!canDelete) {
            return R.error("仅草稿或进行中的盘点单可删除，已完成的盘点单禁止删除");
        }
        stockCheckService.removeById(id);
        return R.success("删除成功");
    }

    /**
     * 盘点冲正：将已完成的盘点单回退到进行中，允许重新录入实盘数量
     * 场景：盘点录入有误（如实盘数量填错），无需新建盘点单，直接回退修正
     */
    @PutMapping("/{id}/rollback")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "盘点冲正", description = "将已完成的盘点单回退到进行中状态，允许重新录入")
    @Parameter(name = "id", description = "盘点单ID", required = true)
    public R<String> rollback(@PathVariable Long id) {
        StockCheck existing = stockCheckService.getById(id);
        if (existing == null) {
            return R.error("盘点单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null || !Objects.equals(currentTenantId, existing.getTenantId())) {
            return R.error("盘点单不属于当前租户");
        }
        if (!StockCheckStatus.DONE.getValue().equals(existing.getStatus())) {
            return R.error("仅已完成的盘点单可冲正");
        }
        // 回退状态到进行中，清空盈亏记录
        existing.setStatus(StockCheckStatus.IN_PROGRESS.getValue());
        existing.setProfitLoss(null);
        stockCheckService.updateById(existing);
        return R.success("已回退到进行中，请重新录入实盘数量");
    }
}


