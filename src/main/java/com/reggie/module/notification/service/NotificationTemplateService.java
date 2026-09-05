package com.reggie.module.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.notification.model.NotificationTemplate;

import java.util.List;
import java.util.Map;

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
     * @param channel  发送渠道：1短信 2APP推送 3短信+推送（可空）
     * @param status   状态：1启用 0停用（可空）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    Page<NotificationTemplate> pageTemplates(int page, int pageSize, String bizType, Integer channel, Integer status, Long tenantId);

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

    /**
     * 查询模板并校验租户归属
     * <p>域4 改造：从 NotificationController 下沉，内置租户校验</p>
     *
     * @param id       模板ID
     * @param tenantId 当前租户ID
     * @return 校验结果：key="ok"/"error"，ok 时附 template，error 时附 message
     */
    Map<String, Object> getTemplateWithTenantCheck(Long id, Long tenantId);

    /**
     * 新增模板并自动设置租户ID
     * <p>域4 改造：从 NotificationController 下沉</p>
     *
     * @param template 模板信息
     * @param tenantId 当前租户ID
     */
    void addTemplateWithTenant(NotificationTemplate template, Long tenantId);

    /**
     * 修改模板并校验租户归属
     * <p>域4 改造：从 NotificationController 下沉，内置租户校验</p>
     *
     * @param template 模板信息
     * @param tenantId 当前租户ID
     * @return 校验结果：key="ok"/"error"，error 时附 message
     */
    Map<String, Object> updateTemplateWithTenant(NotificationTemplate template, Long tenantId);

    /**
     * 启用/停用模板并校验租户归属
     * <p>域4 改造：从 NotificationController 下沉，内置租户校验</p>
     *
     * @param id       模板ID
     * @param status   状态
     * @param tenantId 当前租户ID
     * @return 校验结果：key="ok"/"error"，ok 时附 message（"已启用"/"已停用"），error 时附 message
     */
    Map<String, Object> toggleStatusWithTenant(Long id, Integer status, Long tenantId);

    /**
     * 删除模板并校验租户归属
     * <p>域4 改造：从 NotificationController 下沉，内置租户校验</p>
     *
     * @param id       模板ID
     * @param tenantId 当前租户ID
     * @return 校验结果：key="ok"/"error"，error 时附 message
     */
    Map<String, Object> removeTemplateWithTenant(Long id, Long tenantId);

    /**
     * 按业务类型查询启用的模板（含租户过滤）
     * <p>域4 改造：从 NotificationController 的 sendNotification 下沉</p>
     *
     * @param bizType  业务类型
     * @param tenantId 当前租户ID
     * @return 匹配的启用模板（可能为空）
     */
    NotificationTemplate findByBizTypeAndTenant(String bizType, Long tenantId);
}
