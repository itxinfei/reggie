package com.reggie.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.service.NotificationTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 通知模板服务实现
 * </p>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationTemplateServiceImpl
        extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate>
        implements NotificationTemplateService {

    @Override
    public Page<NotificationTemplate> pageTemplates(int page, int pageSize, String bizType, Long tenantId) {
        Page<NotificationTemplate> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationTemplate::getBizType, bizType);
        }
        if (tenantId != null) {
            wrapper.eq(NotificationTemplate::getTenantId, tenantId);
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
        NotificationTemplate exist = this.getById(template.getId());
        if (exist == null) {
            throw new CustomException("模板不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的通知模板");
        }
        template.setUpdateTime(LocalDateTime.now());
        this.updateById(template);
        log.info("[通知模板] 更新模板：id={}", template.getId());
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        NotificationTemplate exist = this.getById(id);
        if (exist == null) {
            throw new CustomException("模板不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的通知模板");
        }
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        log.info("[通知模板] 切换模板状态：id={}, status={}", id, status);
    }

    @Override
    public void removeTemplate(Long id) {
        NotificationTemplate exist = this.getById(id);
        if (exist == null) {
            throw new CustomException("模板不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的通知模板");
        }
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("is_deleted", 1)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        log.info("[通知模板] 逻辑删除模板：id={}", id);
    }

    @Override
    public Map<String, Object> getTemplateWithTenantCheck(Long id, Long tenantId) {
        NotificationTemplate template = this.getById(id);
        if (template == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "模板不存在");
            return result;
        }
        if (tenantId != null && !tenantId.equals(template.getTenantId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "无权查看其他租户的模板");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("template", template);
        return result;
    }

    @Override
    public void addTemplateWithTenant(NotificationTemplate template, Long tenantId) {
        if (tenantId != null) {
            template.setTenantId(tenantId);
        }
        this.save(template);
        log.info("[通知模板] 新增模板：id={}, name={}, tenantId={}",
                template.getId(), template.getTemplateName(), tenantId);
    }

    @Override
    public Map<String, Object> updateTemplateWithTenant(NotificationTemplate template, Long tenantId) {
        NotificationTemplate existing = this.getById(template.getId());
        if (existing == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "模板不存在");
            return result;
        }
        if (tenantId != null && !tenantId.equals(existing.getTenantId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "无权修改其他租户的模板");
            return result;
        }
        template.setUpdateTime(LocalDateTime.now());
        this.updateById(template);
        log.info("[通知模板] 更新模板：id={}, tenantId={}", template.getId(), tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    @Override
    public Map<String, Object> toggleStatusWithTenant(Long id, Integer status, Long tenantId) {
        NotificationTemplate template = this.getById(id);
        if (template == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "模板不存在");
            return result;
        }
        if (tenantId != null && !tenantId.equals(template.getTenantId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "无权操作其他租户的模板");
            return result;
        }
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("message", status == 1 ? "已启用" : "已停用");
        return result;
    }

    @Override
    public Map<String, Object> removeTemplateWithTenant(Long id, Long tenantId) {
        NotificationTemplate template = this.getById(id);
        if (template == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "模板不存在");
            return result;
        }
        if (tenantId != null && !tenantId.equals(template.getTenantId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "无权删除其他租户的模板");
            return result;
        }
        UpdateWrapper<NotificationTemplate> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("is_deleted", 1)
                .set("update_time", LocalDateTime.now());
        this.update(wrapper);
        log.info("[通知模板] 逻辑删除模板：id={}, tenantId={}", id, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    @Override
    public NotificationTemplate findByBizTypeAndTenant(String bizType, Long tenantId) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getBizType, bizType)
                .eq(NotificationTemplate::getStatus, 1);
        if (tenantId != null) {
            wrapper.eq(NotificationTemplate::getTenantId, tenantId);
        }
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }
}



