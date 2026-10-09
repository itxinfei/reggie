package com.reggie.module.subsidy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.subsidy.dto.SubsidyGrantDTO;
import com.reggie.module.subsidy.model.MealSubsidyRecord;
import com.reggie.module.subsidy.service.SubsidyService;
import com.reggie.module.subsidy.vo.SubsidyAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 餐补管理 Controller（管理端）
 * <p>面向企业内部订餐场景：发放餐补（单人/按部门批量）、账户分页、流水分页。
 * 操作权限与部门管理一致，仅超管可操作。小型餐饮店可不用本能力。</p>
 */
@Slf4j
@RequiresAdmin
@RestController
@RequestMapping("/subsidy/account")
@Tag(name = "餐补管理", description = "企业餐补：发放/账户/流水接口")
public class SubsidyAccountController {

    @Autowired
    private SubsidyService subsidyService;

    /**
     * 账户分页查询（附带顾客姓名/手机号/部门名）。
     */
    @GetMapping("/page")
    @Operation(summary = "餐补账户分页查询")
    public R<IPage<SubsidyAccountVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "手机号（模糊）") @RequestParam(required = false) String phone) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        return R.success(subsidyService.pageAccounts(tenantId, page, pageSize, departmentId, phone));
    }

    /**
     * 发放餐补（单人或按部门批量，departmentId 优先）。
     */
    @PostMapping("/grant")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "发放餐补", description = "按 userId 单发，或按 departmentId 对该部门全部用户批量发放")
    public R<String> grant(@Parameter(description = "发放请求") @Valid @RequestBody SubsidyGrantDTO dto) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        Long operatorId = com.reggie.common.BaseContext.getCurrentId();
        int granted = subsidyService.grant(dto, tenantId, operatorId);
        return R.success("发放成功，共 " + granted + " 人入账");
    }

    /**
     * 流水分页查询。
     */
    @GetMapping("/record/page")
    @Operation(summary = "餐补流水分页查询")
    public R<IPage<MealSubsidyRecord>> recordPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "流水类型：GRANT/CONSUME/REFUND/ADJUST") @RequestParam(required = false) String recordType) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        return R.success(subsidyService.pageRecords(tenantId, page, pageSize, userId, recordType));
    }

    /**
     * 部门对账（JSON 预览）：按部门聚合订单数/金额、餐补发放/核销、企业自付。
     * Excel 导出走 /export/subsidy-department/excel。
     */
    @GetMapping("/reconciliation")
    @Operation(summary = "部门对账预览", description = "按部门聚合有效订单与餐补发放/核销")
    public R<java.util.List<java.util.Map<String, Object>>> reconciliation(
            @Parameter(description = "开始日期 yyyy-MM-dd（含）") @RequestParam String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd（含）") @RequestParam String endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        return R.success(subsidyService.departmentReconciliation(tenantId, startDate, endDate));
    }

    /**
     * 账户汇总（当前租户）：账户数 / 余额合计 / 累计发放 / 累计核销。
     */
    @GetMapping("/stats")
    @Operation(summary = "餐补账户汇总统计")
    public R<Map<String, Object>> stats() {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        Map<String, Object> stats = subsidyService.stats(tenantId);
        return R.success(stats);
    }
}
