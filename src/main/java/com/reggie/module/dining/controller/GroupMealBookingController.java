package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.dining.dto.GroupMealBookingDTO;
import com.reggie.module.dining.model.GroupMealBooking;
import com.reggie.module.dining.service.GroupMealBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 团餐预订 Controller（企业内部订餐）
 * <p>企业按部门批量订餐：创建（价格快照）→ 确认 → 完成；创建/确认等变更操作仅超管，
 * 查询对全部员工开放（与导出模块口径一致）。小型餐饮店可不用本能力。</p>
 */
@Slf4j
@RequireEmployee
@RestController
@RequestMapping("/dining/group-booking")
@Tag(name = "团餐预订", description = "企业团餐：创建/确认/完成/取消与查询接口")
public class GroupMealBookingController {

    @Autowired
    private GroupMealBookingService groupMealBookingService;

    /**
     * 创建团餐预订。
     */
    @PostMapping
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "创建团餐预订", description = "菜单条目名称/单价由服务端快照，总金额为快照合计")
    public R<Map<String, Object>> create(@Parameter(description = "预订请求") @Valid @RequestBody GroupMealBookingDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long operatorId = BaseContext.getCurrentId();
        Long id = groupMealBookingService.createBooking(dto, tenantId, operatorId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", id);
        return R.success(result);
    }

    /**
     * 预订分页查询。
     */
    @GetMapping("/page")
    @Operation(summary = "团餐预订分页查询")
    public R<IPage<GroupMealBooking>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "状态：0=待确认 1=已确认 2=已完成 3=已取消") @RequestParam(required = false) Integer status,
            @Parameter(description = "用餐日期 yyyy-MM-dd") @RequestParam(required = false) String mealDate,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(groupMealBookingService.pageBookings(tenantId, page, pageSize, status, mealDate, departmentId));
    }

    /**
     * 预订详情（含明细条目）。
     */
    @GetMapping("/{id}")
    @Operation(summary = "团餐预订详情", description = "返回 booking + items（含名称/单价快照）")
    public R<Map<String, Object>> detail(@Parameter(description = "预订ID") @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> detail = groupMealBookingService.detail(id, tenantId);
        if (detail == null) {
            return R.error("团餐预订不存在或不属于当前租户");
        }
        return R.success(detail);
    }

    /**
     * 确认预订（待确认 → 已确认）。
     */
    @PutMapping("/{id}/confirm")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "确认预订")
    public R<String> confirm(@Parameter(description = "预订ID") @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        groupMealBookingService.changeStatus(id, GroupMealBooking.STATUS_CONFIRMED, tenantId, BaseContext.getCurrentId());
        return R.success("已确认");
    }

    /**
     * 完成预订（已确认 → 已完成）。
     */
    @PutMapping("/{id}/complete")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "完成预订")
    public R<String> complete(@Parameter(description = "预订ID") @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        groupMealBookingService.changeStatus(id, GroupMealBooking.STATUS_COMPLETED, tenantId, BaseContext.getCurrentId());
        return R.success("已完成");
    }

    /**
     * 取消预订（待确认/已确认 → 已取消）。
     */
    @PutMapping("/{id}/cancel")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "取消预订", description = "待确认/已确认状态可取消，已完成/已取消不可再取消")
    public R<String> cancel(@Parameter(description = "预订ID") @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        groupMealBookingService.changeStatus(id, GroupMealBooking.STATUS_CANCELLED, tenantId, BaseContext.getCurrentId());
        return R.success("已取消");
    }
}
