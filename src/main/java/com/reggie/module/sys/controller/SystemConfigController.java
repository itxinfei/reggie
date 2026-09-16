package com.reggie.module.sys.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.sys.model.SystemConfig;
import com.reggie.module.sys.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reggie.module.sys.dto.SystemConfigSaveDTO;
import com.reggie.module.sys.dto.SystemConfigUpdateDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统配置管理Controller
 */
@RequiresAdmin
@RestController
@RequestMapping("/sys/config")
@Tag(name = "系统管理-系统配置", description = "系统配置CRUD接口")
@Slf4j
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 所有配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "配置列表")
    public R<List<SystemConfig>> list() {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(systemConfigService.listByTenantId(tenantId));
    }

    /**
     * 根路径 GET — 兼容前端直接访问 /sys/config
     */
    @GetMapping
    @Operation(summary = "配置列表", description = "获取所有系统配置列表")
    public R<List<SystemConfig>> rootList() {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(systemConfigService.listByTenantId(tenantId));
    }

    /**
     * 根路径 PUT — 兼容前端 PUT /sys/config
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量更新配置", description = "批量更新系统配置项")
    public R<String> rootBatchUpdate(
            @Parameter(description = "系统配置列表（configKey/configValue），兼容单对象与数组两种形态", required =
                    true) @Valid @RequestBody Object body) {
        // 修改点：根路径 PUT 此前仅接收 List<SystemConfig>，前端 sys.js configUpdate 发送单对象时
        // Jackson 将对象反序列化到 List 参数失败返回 400。改为 Object 兼容单对象与数组两种形态。
        List<SystemConfig> configs = null;
        if (body instanceof List) {
            // 修改点：泛型擦除后逐个转换为 SystemConfig，非 SystemConfig 元素忽略并告警
            for (Object element : (List<?>) body) {
                if (element instanceof SystemConfig) {
                    if (configs == null) {
                        configs = new java.util.ArrayList<>();
                    }
                    configs.add((SystemConfig) element);
                } else {
                    log.warn("根路径 PUT 配置批量更新：忽略非 SystemConfig 元素 {}", element);
                }
            }
        } else if (body instanceof SystemConfig) {
            SystemConfig single = (SystemConfig) body;
            configs = new java.util.ArrayList<>();
            configs.add(single);
        } else {
            log.warn("根路径 PUT 配置批量更新：无法识别的请求体类型 {}，已忽略",
                    body == null ? "null" : body.getClass().getName());
        }
        if (configs != null) {
            for (SystemConfig config : configs) {
                if (config.getConfigKey() != null) {
                    systemConfigService.setConfig(
                            BaseContext.getCurrentTenantId(),
                            config.getConfigKey(),
                            config.getConfigValue()
                    );
                }
            }
        }
        return R.success("配置批量更新成功");
    }

    /**
     * 配置列表（分页）— 兼容前端 GET /sys/config
     */
    @GetMapping("/page")
    @Operation(summary = "配置分页查询", description = "分页查询系统配置")
    public R<Page<SystemConfig>> page(
                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "配置键") @RequestParam(required = false) String configKey,
            @Parameter(description = "配置类型：1=功能开关 2=运营参数 3=显示设置 4=其他") @RequestParam(required = false) Integer configType,
            @Parameter(description = "关键字（配置值/说明模糊搜索）") @RequestParam(required = false) String keyword) {
        // 修改点：补 configType 精确筛选 + keyword 模糊搜索（原仅支持 configKey）
        Page<SystemConfig> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (configKey != null && !configKey.trim().isEmpty()) {
            wrapper.like(SystemConfig::getConfigKey, configKey.trim());
        }
        if (configType != null) {
            wrapper.eq(SystemConfig::getConfigType, configType);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SystemConfig::getConfigValue, keyword.trim())
                    .or().like(SystemConfig::getDescription, keyword.trim()));
        }
        wrapper.orderByDesc(SystemConfig::getUpdateTime);
        systemConfigService.page(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 配置统计（按 configType 聚合计数）
     */
    @GetMapping("/stats")
    @Operation(summary = "配置统计", description = "按类型统计配置数量")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<SystemConfig> all = systemConfigService.listByTenantId(tenantId);
        long total = all.size();
        long type1 = 0, type2 = 0, type3 = 0, type4 = 0;
        for (SystemConfig c : all) {
            Integer t = c.getConfigType();
            if (t == null || t == 4) { type4++; }
            else if (t == 1) { type1++; }
            else if (t == 2) { type2++; }
            else if (t == 3) { type3++; }
            else { type4++; }
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("type1", type1);
        stats.put("type2", type2);
        stats.put("type3", type3);
        stats.put("type4", type4);
        return R.success(stats);
    }

    /**
     * 获取单个配置 — 兼容前端 GET /sys/config/{key}
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "获取配置值", description = "根据配置键获取配置值")
    public R<String> getConfig(
                        @Parameter(description = "配置键") @PathVariable String configKey) {
        String value = systemConfigService.getConfig(configKey);
        return R.success(value);
    }

    /**
     * 新增配置
     * <p>租户安全：使用 SystemConfigSaveDTO 仅接收 configKey/configValue，
     * tenantId 由 Service 层通过 BaseContext 强制设置，前端无法篡改。</p>
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增配置", description = "创建系统配置")
    public R<String> add(
            @Parameter(description = "配置信息") @Valid @RequestBody SystemConfigSaveDTO dto) {
        systemConfigService.addTenantConfig(dto.getConfigKey(), dto.getConfigValue());
        return R.success("配置创建成功");
    }

    /**
     * 修改配置
     * <p>租户安全：使用 SystemConfigUpdateDTO，Service 层先校验归属再更新，
     * 仅允许修改 configValue，绕过全实体覆盖漏洞。</p>
     */
    @PutMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改配置", description = "更新系统配置")
    public R<String> update(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Parameter(description = "配置信息") @Valid @RequestBody SystemConfigUpdateDTO dto) {
        systemConfigService.updateTenantConfig(id, dto.getConfigKey(), dto.getConfigValue());
        return R.success("配置更新成功");
    }

    /**
     * 批量更新配置（前端表单提交用）
     */
    @PutMapping("/batch")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量更新配置", description = "批量更新系统配置项")
    public R<String> batchUpdate(
            @Parameter(description = "配置列表") @Valid @RequestBody List<SystemConfig> configs) {
        if (configs != null && !configs.isEmpty()) {
            for (SystemConfig config : configs) {
                if (config.getConfigKey() != null) {
                    systemConfigService.setConfig(
                            BaseContext.getCurrentTenantId(),
                            config.getConfigKey(),
                            config.getConfigValue()
                    );
                }
            }
        }
        return R.success("配置批量更新成功");
    }

    /**
     * 删除配置
     * <p>租户安全：Service 层先校验该 id 的配置属于当前租户，再删除。</p>
     */
    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除配置", description = "删除指定系统配置")
    @Parameter(description = "配置ID")
    public R<String> delete(@Parameter(description = "配置ID") @PathVariable Long id) {
        systemConfigService.deleteTenantConfig(id);
        return R.success("配置删除成功");
    }
}





