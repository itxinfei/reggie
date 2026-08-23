package com.reggie.module.franchise.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.franchise.model.FranchiseContract;
import com.reggie.module.franchise.service.FranchiseContractService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;

import javax.validation.Valid;
import java.util.List;

/**
 * 加盟合同管理（含抽成规则）
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@RestController
@RequestMapping("/api/franchise/contract")
@Tag(name = "加盟管理-加盟合同", description = "加盟合同（含抽成规则）CRUD 接口")
@RequiresPermission("franchise:manage")
public class FranchiseContractController {

    @Autowired
    private FranchiseContractService franchiseContractService;

    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增加盟合同", description = "新增加盟合同，自动关联当前租户")
    public R<FranchiseContract> save(@Valid @RequestBody FranchiseContract contract) {
        contract.setTenantId(BaseContext.getCurrentTenantId());
        if (contract.getSettleCycle() == null) {
            contract.setSettleCycle(FranchiseContract.SETTLE_CYCLE_MONTHLY);
        }
        if (contract.getStatus() == null) {
            contract.setStatus(FranchiseContract.STATUS_ACTIVE);
        }
        franchiseContractService.save(contract);
        return R.success(contract);
    }

    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改加盟合同", description = "更新合同与抽成规则，先校验租户归属")
    public R<String> update(@Valid @RequestBody FranchiseContract contract) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        FranchiseContract exist = franchiseContractService.getById(contract.getId());
        if (exist == null) {
            return R.error("加盟合同不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("加盟合同不属于当前租户");
        }
        exist.setFranchiseeId(contract.getFranchiseeId());
        exist.setContractNo(contract.getContractNo());
        exist.setStartDate(contract.getStartDate());
        exist.setEndDate(contract.getEndDate());
        exist.setCommissionType(contract.getCommissionType());
        exist.setCommissionRate(contract.getCommissionRate());
        exist.setCommissionAmount(contract.getCommissionAmount());
        exist.setSettleCycle(contract.getSettleCycle());
        exist.setStatus(contract.getStatus());
        exist.setRemark(contract.getRemark());
        franchiseContractService.updateById(exist);
        return R.success("修改成功");
    }

    @DeleteMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除加盟合同", description = "删除合同（逻辑删除），逐条校验租户归属")
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        for (Long id : ids) {
            FranchiseContract exist = franchiseContractService.getById(id);
            if (exist == null) {
                continue;
            }
            if (!tenantId.equals(exist.getTenantId())) {
                throw new CustomException("加盟合同中存在不属于当前租户的记录");
            }
            franchiseContractService.removeById(id);
        }
        return R.success("删除成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询合同详情")
    public R<FranchiseContract> getById(@PathVariable Long id) {
        FranchiseContract contract = franchiseContractService.getById(id);
        if (contract == null) {
            return R.error("加盟合同不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(contract.getTenantId())) {
            return R.error("无权查看其他租户的加盟合同信息");
        }
        return R.success(contract);
    }

    @GetMapping("/list")
    @Operation(summary = "查询生效合同列表", description = "查询当前租户全部生效合同（供结算生成下拉选择）")
    public R<List<FranchiseContract>> list() {
        LambdaQueryWrapper<FranchiseContract> qw = new LambdaQueryWrapper<>();
        qw.eq(FranchiseContract::getTenantId, BaseContext.getCurrentTenantId());
        qw.orderByDesc(FranchiseContract::getCreateTime);
        return R.success(franchiseContractService.list(qw));
    }

    @GetMapping("/page")
    @Operation(summary = "合同分页查询", description = "支持按加盟商、状态筛选")
    public R<Page<FranchiseContract>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "加盟商ID") @RequestParam(required = false) Long franchiseeId,
            @Parameter(description = "合同状态") @RequestParam(required = false) Integer status) {
        Page<FranchiseContract> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<FranchiseContract> qw = new LambdaQueryWrapper<>();
        qw.eq(FranchiseContract::getTenantId, BaseContext.getCurrentTenantId());
        qw.eq(franchiseeId != null, FranchiseContract::getFranchiseeId, franchiseeId);
        qw.eq(status != null, FranchiseContract::getStatus, status);
        qw.orderByDesc(FranchiseContract::getCreateTime);
        franchiseContractService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}
