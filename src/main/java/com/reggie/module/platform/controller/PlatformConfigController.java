package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
public class PlatformConfigController {

    @Autowired
    private PlatformConfigService platformConfigService;

    /** 列表（凭据脱敏，分页） */
    @GetMapping("/list")
    @RequiresPermission("platform:manage")
    public R<IPage<PlatformConfig>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<PlatformConfig> pageReq = PageUtils.of(page, pageSize);
        return R.success(platformConfigService.pageMasked(pageReq));
    }

    /** 详情（凭据脱敏） */
    @GetMapping("/detail")
    @RequiresPermission("platform:manage")
    public R<PlatformConfig> detail(@RequestParam Long id) {
        return R.success(platformConfigService.getMaskedById(id));
    }

    /** 新增 */
    @PostMapping("/add")
    @RequiresPermission("platform:manage")
    public R<PlatformConfig> add(@RequestBody PlatformConfig config) {
        if (platformConfigService.existsByTypeAndShop(config.getPlatformType(), config.getShopId(), null)) {
            return R.error("该平台同一门店已存在接入配置");
        }
        return R.success(platformConfigService.addConfig(config));
    }

    /** 更新 */
    @PostMapping("/update")
    @RequiresPermission("platform:manage")
    public R<Boolean> update(@RequestBody PlatformConfig config) {
        if (config.getId() == null) {
            return R.error("缺少主键 ID");
        }
        if (platformConfigService.existsByTypeAndShop(config.getPlatformType(), config.getShopId(), config.getId())) {
            return R.error("该平台同一门店已存在接入配置");
        }
        return R.success(platformConfigService.updateConfig(config));
    }

    /** 删除（逻辑删除） */
    @PostMapping("/delete")
    @RequiresPermission("platform:manage")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.success(platformConfigService.removeById(id));
    }

    /** 启用 / 停用 */
    @PostMapping("/toggle")
    @RequiresPermission("platform:manage")
    public R<Boolean> toggle(@RequestParam Long id, @RequestParam Integer enabled) {
        return R.success(platformConfigService.setEnabled(id, enabled));
    }
}
