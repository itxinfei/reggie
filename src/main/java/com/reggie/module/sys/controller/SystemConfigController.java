package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.sys.entity.SystemConfig;
import com.reggie.module.sys.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 系统配置管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/sys/config")
@Tag(name = "系统管理-系统配置", description = "系统配置CRUD接口")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 配置列表（分页）
     */
    @GetMapping("/page")
    @Operation(summary = "配置分页查询")
    public R<Page<SystemConfig>> page(int page, int pageSize, String configKey) {
        Page<SystemConfig> pageInfo = new Page<>(page, pageSize);
        // 简化查询，使用Service层方法
        List<SystemConfig> all = systemConfigService.list();
        return R.success(pageInfo);
    }

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
     * 获取单个配置
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "获取配置值")
    public R<String> getConfig(@PathVariable String configKey) {
        String value = systemConfigService.getConfig(configKey);
        return R.success(value);
    }

    /**
     * 新增配置
     */
    @PostMapping
    @Operation(summary = "新增配置")
    public R<String> add(@Valid @RequestBody SystemConfig config) {
        systemConfigService.save(config);
        return R.success("配置创建成功");
    }

    /**
     * 修改配置
     */
    @PutMapping
    @Operation(summary = "修改配置")
    public R<String> update(@Valid @RequestBody SystemConfig config) {
        systemConfigService.updateById(config);
        return R.success("配置更新成功");
    }

    /**
     * 批量更新配置（前端表单提交用）
     */
    @PutMapping("/batch")
    @Operation(summary = "批量更新配置")
    public R<String> batchUpdate(@RequestBody List<SystemConfig> configs) {
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
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    public R<String> delete(@PathVariable Long id) {
        systemConfigService.removeById(id);
        return R.success("配置删除成功");
    }
}
