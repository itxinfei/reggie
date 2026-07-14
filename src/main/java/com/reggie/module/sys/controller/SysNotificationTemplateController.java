package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.sys.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 通知模板管理Controller
 * 系统管理模块下的通知模板管理，替代原来的独立消息通知页面
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/sys/template")
@Tag(name = "系统管理-通知模板", description = "通知模板CRUD接口")
public class SysNotificationTemplateController {

    @Resource
    private NotificationTemplateMapper templateMapper;

    @Autowired
    private PermissionService permissionService;

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
            @Parameter(description = "页码") int page,
            @Parameter(description = "每页条数") int pageSize,
            @Parameter(description = "业务类型") String bizType) {
        Page<NotificationTemplate> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationTemplate::getBizType, bizType);
        }
        wrapper.orderByDesc(NotificationTemplate::getCreateTime);
        templateMapper.selectPage(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 所有模板列表
     * @param bizType 业务类型
     * @return 模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "模板列表", description = "查询所有通知模板")
    public R<java.util.List<NotificationTemplate>> list(
            @Parameter(description = "业务类型") String bizType) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationTemplate::getBizType, bizType);
        }
        wrapper.eq(NotificationTemplate::getIsDeleted, 0)
               .orderByDesc(NotificationTemplate::getCreateTime);
        return R.success(templateMapper.selectList(wrapper));
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
        NotificationTemplate template = templateMapper.selectById(id);
        return R.success(template);
    }

    /**
     * 新增模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增模板", description = "创建通知模板")
    public R<String> add(
            @Parameter(description = "模板信息") @RequestBody NotificationTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setIsDeleted(0);
        templateMapper.insert(template);
        return R.success("模板创建成功");
    }

    /**
     * 修改模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改模板", description = "更新通知模板信息")
    public R<String> update(
            @Parameter(description = "模板信息") @RequestBody NotificationTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success("模板更新成功");
    }

    /**
     * 启用/停用模板
     * @param id 模板ID
     * @param status 状态：1启用 0停用
     * @return 操作结果
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换模板状态", description = "启用或停用通知模板")
    public R<String> toggle(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success(status == 1 ? "模板已启用" : "模板已停用");
    }

    /**
     * 删除模板
     * @param id 模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板", description = "逻辑删除通知模板")
    public R<String> delete(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setIsDeleted(1);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success("模板删除成功");
    }
}
