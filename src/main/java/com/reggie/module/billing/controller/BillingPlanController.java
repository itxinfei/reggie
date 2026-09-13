package com.reggie.module.billing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.RateLimit;
import com.reggie.module.billing.model.BillingPlan;
import com.reggie.module.billing.service.BillingPlanService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * SaaS套餐方案管理。写操作仅平台超管；查询对登录租户开放（租户选购套餐需要读取上架列表），
 * 故鉴权采用方法级而非类级，避免拦截租户侧的公开读取端点。
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/plan")
@Tag(name = "SaaS计费-套餐方案", description = "套餐目录查询/维护")
public class BillingPlanController {

    @Autowired
    private BillingPlanService billingPlanService;

    /**
     * 分页查询套餐（管理后台，含下架套餐）。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param keyword  关键字
     * @param status   状态
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "套餐分页查询")
    public R<Page<BillingPlan>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.success(billingPlanService.pageQuery(page, pageSize, keyword, status));
    }

    /**
     * 上架套餐列表（租户选购）。
     *
     * @return 套餐列表
     */
    @GetMapping("/onShelf")
    @Operation(summary = "上架套餐列表（租户选购）")
    public R<List<BillingPlan>> onShelf() {
        return R.success(billingPlanService.listOnShelf());
    }

    /**
     * 套餐详情。
     *
     * @param id 套餐ID
     * @return 套餐
     */
    @GetMapping("/{id}")
    @Operation(summary = "套餐详情")
    public R<BillingPlan> getById(@Parameter(description = "套餐ID", required = true) @PathVariable Long id) {
        return R.success(billingPlanService.getById(id));
    }

    /**
     * 新增套餐（超管）。
     *
     * @param plan 套餐
     * @return 保存后的套餐
     */
    @PostMapping
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "新增套餐（超管）")
    public R<BillingPlan> save(@RequestBody @Valid BillingPlan plan) {
        return R.success(billingPlanService.createPlan(plan));
    }

    /**
     * 修改套餐（超管）。
     *
     * @param plan 套餐
     * @return 结果
     */
    @PutMapping
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "修改套餐（超管）")
    public R<String> update(@RequestBody @Valid BillingPlan plan) {
        billingPlanService.updatePlan(plan);
        return R.success("修改成功");
    }

    /**
     * 上架/下架（超管）。
     *
     * @param id     套餐ID
     * @param status 目标状态
     * @return 结果
     */
    @PutMapping("/status/{id}")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "套餐上下架（超管）")
    public R<String> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        billingPlanService.toggleStatus(id, status);
        return R.success("状态更新成功");
    }

    /**
     * 删除套餐（超管，逻辑删除）。
     *
     * @param id 套餐ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    @RequiresAdmin
    @Operation(summary = "删除套餐（超管）")
    public R<String> delete(@PathVariable Long id) {
        billingPlanService.removeById(id);
        return R.success("删除成功");
    }
}
