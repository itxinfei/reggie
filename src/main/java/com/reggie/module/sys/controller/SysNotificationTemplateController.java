package com.reggie.module.sys.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.service.NotificationTemplateService;
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

import java.util.List;

/**
 * <p>
 * 通知模板管理Controller
 * 系统管理模块下的通知模板管理，替代原来的独立消息通知页面
 * 修改点：原 Controller 直接调用 NotificationTemplateMapper（违反分层规范），
 * 现统一改为调用 NotificationTemplateService，数据库访问收敛到 Service 层。
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequiresAdmin
@RestController
@RequestMapping("/sys/template")
@Tag(name = "系统管理-通知模板", description = "通知模板CRUD接口")
public class SysNotificationTemplateController {

    @Autowired
    private NotificationTemplateService templateService;

    /**
     * 模板分页查询
     * @param page 页码
     * @param pageSize 每页条数
     * @param bizType 业务类型
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "模板分页查询", description = "分页查询通知模板")
    public R<Page<NotificationTemplate>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "业务类型") String bizType,
            @Parameter(description = "发送渠道：1短信 2APP推送 3短信+推送") Integer channel,
            @Parameter(description = "状态：1启用 0停用") Integer status) {
        return R.success(templateService.pageTemplates(page, PageUtils.cap(pageSize), bizType, channel, status, BaseContext.getCurrentTenantId()));
    }

    /**
     * 所有模板列表
     * @param bizType 业务类型
     * @return 模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "模板列表", description = "查询所有通知模板")
    public R<List<NotificationTemplate>> list(
            @Parameter(description = "业务类型") String bizType) {
        return R.success(templateService.listTemplates(bizType));
    }

    /**
     * 模板详情
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "模板详情", description = "获取通知模板的详细信息")
    public R<NotificationTemplate> detail(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        return R.success(templateService.getTemplate(id));
    }

    /**
     * 新增模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增模板", description = "创建通知模板")
    public R<String> add(
            @Parameter(description = "模板信息") @RequestBody NotificationTemplate template) {
        templateService.addTemplate(template);
        return R.success("模板创建成功");
    }

    /**
     * 修改模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改模板", description = "更新通知模板信息")
    public R<String> update(
            @Parameter(description = "模板信息") @RequestBody NotificationTemplate template) {
        templateService.updateTemplate(template);
        return R.success("模板更新成功");
    }

    /**
     * 启用/停用模板
     * @param id 模板ID
     * @param status 状态：1启用 0停用
     * @return 操作结果
     */
    @PutMapping("/{id}/toggle")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "切换模板状态", description = "启用或停用通知模板")
    public R<String> toggle(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        templateService.toggleStatus(id, status);
        return R.success(status == 1 ? "模板已启用" : "模板已停用");
    }

    /**
     * 删除模板
     * @param id 模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除模板", description = "逻辑删除通知模板")
    public R<String> delete(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        templateService.removeTemplate(id);
        return R.success("模板删除成功");
    }
}


