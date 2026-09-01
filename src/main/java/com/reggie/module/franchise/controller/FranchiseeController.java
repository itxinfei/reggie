package com.reggie.module.franchise.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.franchise.model.Franchisee;
import com.reggie.module.franchise.service.FranchiseeService;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * 加盟商管理
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@RestController
@RequestMapping("/api/franchise/franchisee")
@Tag(name = "加盟管理-加盟商", description = "加盟商 CRUD 接口")
@RequiresPermission("franchise:manage")
public class FranchiseeController {

    @Autowired
    private FranchiseeService franchiseeService;

    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增加盟商", description = "新增加盟商，自动关联当前租户")
    public R<Franchisee> save(@Valid @RequestBody Franchisee franchisee) {
        franchisee.setTenantId(BaseContext.getCurrentTenantId());
        if (franchisee.getStatus() == null) {
            franchisee.setStatus(Franchisee.STATUS_ENABLED);
        }
        franchiseeService.save(franchisee);
        return R.success(franchisee);
    }

    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改加盟商", description = "更新加盟商信息，仅限本租户")
    public R<String> update(@Valid @RequestBody Franchisee franchisee) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        Franchisee exist = franchiseeService.getById(franchisee.getId());
        if (exist == null) {
            return R.error("加盟商不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("加盟商不属于当前租户");
        }
        exist.setName(franchisee.getName());
        exist.setContactPerson(franchisee.getContactPerson());
        exist.setContactPhone(franchisee.getContactPhone());
        exist.setAddress(franchisee.getAddress());
        exist.setStatus(franchisee.getStatus());
        exist.setRemark(franchisee.getRemark());
        franchiseeService.updateById(exist);
        return R.success("修改成功");
    }

    @DeleteMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除加盟商", description = "删除加盟商（逻辑删除），逐条校验租户归属")
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        for (Long id : ids) {
            Franchisee exist = franchiseeService.getById(id);
            if (exist == null) {
                continue;
            }
            if (!tenantId.equals(exist.getTenantId())) {
                throw new CustomException("加盟商中存在不属于当前租户的记录");
            }
            franchiseeService.removeById(id);
        }
        return R.success("删除成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询加盟商详情")
    public R<Franchisee> getById(@PathVariable Long id) {
        Franchisee franchisee = franchiseeService.getById(id);
        if (franchisee == null) {
            return R.error("加盟商不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(franchisee.getTenantId())) {
            return R.error("无权查看其他租户的加盟商信息");
        }
        return R.success(franchisee);
    }

    @GetMapping("/list")
    @Operation(summary = "查询加盟商列表", description = "查询当前租户全部启用加盟商")
    public R<List<Franchisee>> list() {
        LambdaQueryWrapper<Franchisee> qw = new LambdaQueryWrapper<>();
        qw.eq(Franchisee::getTenantId, BaseContext.getCurrentTenantId());
        qw.orderByDesc(Franchisee::getCreateTime);
        return R.success(franchiseeService.list(qw));
    }

    @GetMapping("/page")
    @Operation(summary = "加盟商分页查询", description = "支持按名称、状态筛选")
    public R<Page<Franchisee>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "名称（模糊）") @RequestParam(required = false) String name,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        Page<Franchisee> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Franchisee> qw = new LambdaQueryWrapper<>();
        qw.eq(Franchisee::getTenantId, BaseContext.getCurrentTenantId());
        qw.like(name != null && !name.trim().isEmpty(), Franchisee::getName, name);
        qw.eq(status != null, Franchisee::getStatus, status);
        qw.orderByDesc(Franchisee::getCreateTime);
        franchiseeService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @GetMapping("/stats")
    @RequiresPermission("franchise:manage")
    @Operation(summary = "加盟商统计", description = "返回总数/启用/禁用/关联合同数，按当前租户聚合")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        return R.success(franchiseeService.statFranchisees(tenantId));
    }
}