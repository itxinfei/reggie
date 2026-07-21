package com.reggie.module.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.notification.model.NotificationTemplate;

import java.util.List;

/**
 * <p>
 * 通知模板服务接口
 * 封装通知模板的 CRUD 与状态切换，Controller 不再直接操作 Mapper，统一收敛到 Service 层。
 * </p>
 *
 * @author reggie
 * @since 2026-07-20
 */
public interface NotificationTemplateService extends IService<NotificationTemplate> {

    /**
     * 模板分页查询
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @param bizType  业务类型（可空）
     * @return 分页结果
     */
    Page<NotificationTemplate> pageTemplates(int page, int pageSize, String bizType);

    /**
     * 模板列表查询
     *
     * @param bizType 业务类型（可空）
     * @return 模板列表
     */
    List<NotificationTemplate> listTemplates(String bizType);

    /**
     * 模板详情
     *
     * @param id 模板ID
     * @return 模板实体（可能为空）
     */
    NotificationTemplate getTemplate(Long id);

    /**
     * 新增模板
     *
     * @param template 模板信息
     */
    void addTemplate(NotificationTemplate template);

    /**
     * 修改模板
     *
     * @param template 模板信息
     */
    void updateTemplate(NotificationTemplate template);

    /**
     * 启用/停用模板
     * <p>修改点：使用 UpdateWrapper 精准更新状态字段，避免全字段覆盖风险</p>
     *
     * @param id     模板ID
     * @param status 状态：1启用 0停用
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 逻辑删除模板
     * <p>修改点：使用 UpdateWrapper 精准置位 is_deleted，避免全字段覆盖风险</p>
     *
     * @param id 模板ID
     */
    void removeTemplate(Long id);
}
