package com.reggie.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.service.NotificationTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 通知模板服务实现
 * </p>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
/**
 * NotificationTemplate service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationTemplateServiceImpl
        extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate>
        implements NotificationTemplateService {

    @Override
    public Page<NotificationTemplate> pageTemplates(int page, int pageSize, String bizType) {
        Page<NotificationTemplate> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationTemplate::getBizType, bizType);
        }
        wrapper.orderByDesc(NotificationTemplate::getCreateTime);
        return this.page(pageInfo, wrapper);
    }

    @Override
    public List<NotificationTemplate> listTemplates(String bizType) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationTemplate::getBizType, bizType);
        }
        wrapper.orderByDesc(NotificationTemplate::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public NotificationTemplate getTemplate(Long id) {
        return this.getById(id);
    }

    @Override
    public void addTemplate(NotificationTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setIsDeleted(0);
        this.save(template);
        log.info("[通知模板] 新增模板：id={}, name={}", template.getId(), template.getTemplateName());
    }

    @Override
    public void updateTemplate(NotificationTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        this.updateById(template);
        log.info("[通知模板] 更新模板：id={}", template.getId());
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        // 修改点：改用 UpdateWrapper 精准更新状态字段，避免 updateById 全字段覆盖风险
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        log.info("[通知模板] 切换模板状态：id={}, status={}", id, status);
    }

    @Override
    public void removeTemplate(Long id) {
        // 修改点：改用 UpdateWrapper 精准置位逻辑删除，避免 updateById 全字段覆盖风险
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("is_deleted", 1)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        log.info("[通知模板] 逻辑删除模板：id={}", id);
    }
}



