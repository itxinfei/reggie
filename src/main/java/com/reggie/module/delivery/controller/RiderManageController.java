package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.service.DeliveryTrackingService;
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

import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 后台骑手管理控制器（员工端）。
 * <p>
 * 与骑手自助接口 {@code /api/rider/**}（{@code @RequireRider}）严格区分：
 * 本控制器面向门店管理员，提供骑手账号的分页查询、创建（设置初始密码）、
 * 资料编辑、重置密码与删除，全部走员工会话鉴权与租户隔离。
 * </p>
 *
 * @author reggie
 * @since 2026-09-27
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery/rider")
@Tag(name = "骑手管理（后台）", description = "骑手账号分页/创建/编辑/重置密码/删除")
@RequireEmployee
public class RiderManageController {

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    /**
     * 骑手分页查询。
     * @param page 页码
     * @param pageSize 每页条数
     * @param name 姓名（模糊）
     * @param phone 手机号（模糊）
     * @param status 状态 0-离线 1-在线 2-忙碌
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "骑手分页查询")
    public R<Page<Rider>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "姓名（模糊）") @RequestParam(required = false) String name,
            @Parameter(description = "手机号（模糊）") @RequestParam(required = false) String phone,
            @Parameter(description = "状态：0-离线 1-在线 2-忙碌") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Page<Rider> result = deliveryTrackingService.pageRiders(
                page, PageUtils.cap(pageSize), name, phone, status, tenantId);
        return R.success(result);
    }

    /**
     * 新增骑手。
     * @param body 含 name、phone、password
     * @return 新建骑手
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增骑手", description = "设置骑手登录手机号与初始密码（BCrypt 加密入库）")
    public R<Rider> create(@RequestBody Map<String, String> body) {
        Rider rider = deliveryTrackingService.createRider(
                body == null ? null : body.get("name"),
                body == null ? null : body.get("phone"),
                body == null ? null : body.get("password"));
        return R.success(rider);
    }

    /**
     * 编辑骑手资料。
     * @param body 含 id、name、phone、avatar（可选）
     * @return 更新后的骑手
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "编辑骑手资料", description = "仅修改姓名/手机号/头像，不涉及密码与工作状态")
    public R<Rider> update(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null) {
            return R.error("骑手ID不能为空");
        }
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
        String phone = body.get("phone") == null ? null : String.valueOf(body.get("phone"));
        String avatar = body.get("avatar") == null ? null : String.valueOf(body.get("avatar"));
        return R.success(deliveryTrackingService.updateRiderProfile(id, name, phone, avatar));
    }

    /**
     * 重置骑手密码。
     * @param id 骑手ID
     * @param body 含 password
     * @return 结果
     */
    @PutMapping("/{id}/password")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "重置骑手密码")
    public R<String> resetPassword(@Parameter(description = "骑手ID", required = true) @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        String password = body == null ? null : body.get("password");
        boolean ok = deliveryTrackingService.resetRiderPassword(id, password);
        return ok ? R.success("密码重置成功") : R.error("密码重置失败");
    }

    /**
     * 删除骑手。
     * @param id 骑手ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除骑手")
    public R<String> delete(@Parameter(description = "骑手ID", required = true) @PathVariable Long id) {
        boolean ok = deliveryTrackingService.deleteRider(id);
        return ok ? R.success("删除成功") : R.error("删除失败，骑手不存在");
    }
}
