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
 * 通知模板管理Controller
 * 系统管理模块下的通知模板管理，替代原来的独立消息通知页面
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
     */
    @GetMapping("/page")
    @Operation(summary = "模板分页查询")
    public R<Page<NotificationTemplate>> page(int page, int pageSize, String bizType) {
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
     */
    @GetMapping("/list")
    @Operation(summary = "模板列表")
    public R<java.util.List<NotificationTemplate>> list(String bizType) {
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
     */
    @GetMapping("/{id}")
    @Operation(summary = "模板详情")
    public R<NotificationTemplate> detail(@PathVariable Long id) {
        NotificationTemplate template = templateMapper.selectById(id);
        return R.success(template);
    }

    /**
     * 新增模板
     */
    @PostMapping
    @Operation(summary = "新增模板")
    public R<String> add(@RequestBody NotificationTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setIsDeleted(0);
        templateMapper.insert(template);
        return R.success("模板创建成功");
    }

    /**
     * 修改模板
     */
    @PutMapping
    @Operation(summary = "修改模板")
    public R<String> update(@RequestBody NotificationTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success("模板更新成功");
    }

    /**
     * 启用/停用模板
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换模板状态")
    public R<String> toggle(@PathVariable Long id, @RequestParam Integer status) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success(status == 1 ? "模板已启用" : "模板已停用");
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public R<String> delete(@PathVariable Long id) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setIsDeleted(1);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.success("模板删除成功");
    }
}
