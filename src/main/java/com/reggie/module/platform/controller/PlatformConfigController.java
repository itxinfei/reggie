package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 外卖平台接入配置 Controller
 * <p>
 * 后台管理接口，仅员工可访问；管理动作需 platform:manage 权限（超管自动放行）。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@RestController
@RequestMapping("/admin/platform/config")
@RequireEmployee
@Tag(name = "平台配置管理")
public class PlatformConfigController {

    @Autowired
    private PlatformConfigService platformConfigService;

    /**
     * 分页查询平台接入配置
     * <p>返回的凭据（appKey / appSecret）已做脱敏处理，仅展示首尾字符，避免密钥泄露</p>
     */
    @Operation(summary = "分页查询平台接入配置",
            description = "按启用状态筛选分页查询外卖平台（美团/饿了么/抖音）接入配置列表。"
                    + "返回的凭据已脱敏，不暴露真实密钥；需要真实密钥请走服务层内部调用。")
    @GetMapping("/list")
    @RequiresPermission("platform:manage")
    public R<IPage<PlatformConfig>> list(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，上限 100", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "启用状态筛选：1=已启用，0=已停用，不传表示查询全部", example = "1")
            @RequestParam(required = false) Integer enabled) {
        IPage<PlatformConfig> pageReq = PageUtils.of(page, pageSize);
        return R.success(platformConfigService.pageMasked(pageReq, enabled));
    }

    /**
     * 查询平台接入配置详情（凭据脱敏）
     */
    @Operation(summary = "查询平台接入配置详情",
            description = "按主键查询单个平台接入配置，凭据字段已脱敏。")
    @GetMapping("/detail")
    @RequiresPermission("platform:manage")
    public R<PlatformConfig> detail(
            @Parameter(description = "配置主键 ID", required = true, example = "1")
            @RequestParam Long id) {
        return R.success(platformConfigService.getMaskedById(id));
    }

    /**
     * 新增平台接入配置
     * <p>同一平台 + 同一门店只允许存在一条配置，重复新增会被拒绝</p>
     */
    @Operation(summary = "新增平台接入配置",
            description = "新增外卖平台接入配置。约束：同一平台类型 + 同一门店只能存在一条配置，"
                    + "重复提交返回 code=0 并提示「该平台同一门店已存在接入配置」。")
    @PostMapping("/add")
    @RequiresPermission("platform:manage")
    public R<PlatformConfig> add(@Parameter(description = "平台接入配置（平台类型、门店ID、密钥等）", required = true) @RequestBody PlatformConfig config) {
        if (platformConfigService.existsByTypeAndShop(config.getPlatformType(), config.getShopId(), null)) {
            return R.error("该平台同一门店已存在接入配置");
        }
        return R.success(platformConfigService.addConfig(config));
    }

    /**
     * 更新平台接入配置
     */
    @Operation(summary = "更新平台接入配置",
            description = "按主键更新接入配置。缺少主键 ID 或「平台 + 门店」与其他记录冲突时返回失败。")
    @PostMapping("/update")
    @RequiresPermission("platform:manage")
    public R<Boolean> update(@Parameter(description = "平台接入配置（含ID）", required = true) @RequestBody PlatformConfig config) {
        if (config.getId() == null) {
            return R.error("缺少主键 ID");
        }
        if (platformConfigService.existsByTypeAndShop(config.getPlatformType(), config.getShopId(), config.getId())) {
            return R.error("该平台同一门店已存在接入配置");
        }
        return R.success(platformConfigService.updateConfig(config));
    }

    /**
     * 删除平台接入配置（逻辑删除）
     */
    @Operation(summary = "删除平台接入配置",
            description = "逻辑删除接入配置（is_deleted 标记），不做物理删除，历史同步日志仍可追溯。")
    @PostMapping("/delete")
    @RequiresPermission("platform:manage")
    public R<Boolean> delete(
            @Parameter(description = "配置主键 ID", required = true, example = "1")
            @RequestParam Long id) {
        return R.success(platformConfigService.removeById(id));
    }

    /**
     * 启用 / 停用平台接入配置
     */
    @Operation(summary = "启用或停用平台接入配置",
            description = "启用（enabled=1）或停用（enabled=0）接入配置；停用的平台不会被拉单、同步等定时任务扫描。")
    @PostMapping("/toggle")
    @RequiresPermission("platform:manage")
    public R<Boolean> toggle(
            @Parameter(description = "配置主键 ID", required = true, example = "1")
            @RequestParam Long id,
            @Parameter(description = "目标状态：1=启用，0=停用", required = true, example = "1")
            @RequestParam Integer enabled) {
        return R.success(platformConfigService.setEnabled(id, enabled));
    }

    /**
     * 平台配置统计
     * <p>按启用状态统计总数/已启用/已停用，供前端统计卡片点击筛选使用</p>
     */
    @Operation(summary = "平台接入配置统计",
            description = "统计接入配置总数、已启用数与已停用数，供后台统计卡片展示与快捷筛选。")
    @GetMapping("/stats")
    @RequiresPermission("platform:manage")
    public R<Map<String, Object>> stats() {
        long total = platformConfigService.count();
        long enabledCount = platformConfigService.count(
                new LambdaQueryWrapper<PlatformConfig>().eq(PlatformConfig::getEnabled, 1));
        long disabledCount = platformConfigService.count(
                new LambdaQueryWrapper<PlatformConfig>().eq(PlatformConfig::getEnabled, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("enabledCount", enabledCount);
        result.put("disabledCount", disabledCount);
        return R.success(result);
    }
}
