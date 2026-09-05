package com.reggie.module.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.BaseContext;
import com.reggie.module.notification.dto.BatchSendNotificationDTO;
import com.reggie.module.notification.dto.RegisterDeviceDTO;
import com.reggie.module.notification.dto.SendNotificationDTO;
import com.reggie.module.notification.dto.SendSimpleMessageDTO;
import com.reggie.module.notification.dto.SendToAllUsersDTO;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.service.NotificationRecordService;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.notification.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 消息通知管理控制器
 * 提供通知模板管理、消息发送、发送记录查询等接口
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/notification")
@Tag(name = "消息通知", description = "通知模板管理、消息发送、发送记录查询等接口")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private NotificationTemplateService templateService;

    @Resource
    private NotificationRecordService recordService;

    /** 批量发送目标数量上限，防止一次性投递过大任务压垮下游短信/推送通道 */
    private static final int MAX_BATCH_TARGETS = 1000;

    // ==================== 模板管理 ====================

    /**
     * 分页查询通知模板
     * @param page 页码
     * @param pageSize 每页条数
     * @param bizType 业务类型
     * @return 分页结果
     */
    @GetMapping("/template/page")
    @Operation(summary = "分页查询模板", description = "分页查询通知模板列表，支持按业务类型筛选")
    public R<Page<NotificationTemplate>> templatePage(
                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "业务类型（可选）") @RequestParam(required = false) String bizType,
            @Parameter(description = "发送渠道：1短信 2APP推送 3短信+推送（可选）") @RequestParam(required = false) Integer channel,
            @Parameter(description = "状态：1启用 0停用（可选）") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        // 域4 改造：分页查询下沉到 Service，内置租户过滤
        Page<NotificationTemplate> pageInfo = templateService.pageTemplates(page, pageSize, bizType, channel, status, tenantId);
        return R.success(pageInfo);
    }

    /**
     * 获取模板详情
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/template/{id}")
    @Operation(summary = "查询模板详情", description = "根据ID查询通知模板详情")
    public R<NotificationTemplate> templateDetail(
            @Parameter(description = "模板ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> checkResult = templateService.getTemplateWithTenantCheck(id, tenantId);
        if (Boolean.FALSE.equals(checkResult.get("ok"))) {
            return R.error((String) checkResult.get("message"));
        }
        return R.success((NotificationTemplate) checkResult.get("template"));
    }

    /**
     * 新增通知模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PostMapping("/template")
    @Operation(summary = "新增通知模板", description = "创建新的通知模板")
    public R<String> addTemplate(
            @Parameter(description = "模板信息", required = true) @RequestBody NotificationTemplate template) {
        Long tenantId = BaseContext.getCurrentTenantId();
        // 域4 改造：新增模板下沉到 Service，自动设置租户ID
        templateService.addTemplateWithTenant(template, tenantId);
        return R.success("添加成功");
    }

    /**
     * 修改通知模板
     * @param template 模板信息
     * @return 操作结果
     */
    @PutMapping("/template")
    @Operation(summary = "修改通知模板", description = "更新通知模板信息")
    public R<String> updateTemplate(
            @Parameter(description = "模板信息", required = true) @RequestBody NotificationTemplate template) {
        if (template.getId() == null) {
            return R.error("模板ID不能为空");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> updateResult = templateService.updateTemplateWithTenant(template, tenantId);
        if (Boolean.FALSE.equals(updateResult.get("ok"))) {
            return R.error((String) updateResult.get("message"));
        }
        return R.success("修改成功");
    }

    /**
     * 启用/停用模板
     * @param id 模板ID
     * @param status 状态：1启用 0停用
     * @return 操作结果
     */
    @PutMapping("/template/{id}/status/{status}")
    @Operation(summary = "启用/停用模板", description = "切换通知模板的启用状态")
    public R<String> toggleTemplate(
            @Parameter(description = "模板ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态：1启用 0停用", required = true) @PathVariable Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> toggleResult = templateService.toggleStatusWithTenant(id, status, tenantId);
        if (Boolean.FALSE.equals(toggleResult.get("ok"))) {
            return R.error((String) toggleResult.get("message"));
        }
        return R.success((String) toggleResult.get("message"));
    }

    /**
     * 删除通知模板
     * @param id 模板ID
     * @return 操作结果
     */
    @DeleteMapping("/template/{id}")
    @Operation(summary = "删除通知模板", description = "删除指定的通知模板")
    public R<String> deleteTemplate(
            @Parameter(description = "模板ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> removeResult = templateService.removeTemplateWithTenant(id, tenantId);
        if (Boolean.FALSE.equals(removeResult.get("ok"))) {
            return R.error((String) removeResult.get("message"));
        }
        return R.success("删除成功");
    }

    // ==================== 消息发送 ====================

    /**
     * 发送通知（通用接口）
     * @param body 通知参数（bizType/channel/targets/params/sendTime）
     * @return 发送记录
     */
    @PostMapping("/send")
    @Operation(summary = "发送通知", description = "通用通知发送接口，支持多渠道发送")
    @RequiresPermission("notification:send")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    public R<NotificationRecord> sendNotification(
            @Parameter(description = "通知参数（bizType/channel/targets/params/sendTime）", required = true) @Valid @RequestBody SendNotificationDTO dto) {
        String bizType = dto.getBizType();
        Integer channel = dto.getChannel();
        List<String> targets = dto.getTargets();
        Map<String, String> params = dto.getParams();
        String sendTimeStr = dto.getSendTime();

        if (bizType == null || targets == null || targets.isEmpty()) {
            return R.error("业务类型和目标用户不能为空");
        }

        java.time.LocalDateTime sendTime = null;
        if (sendTimeStr != null && !sendTimeStr.isEmpty()) {
            try {
                sendTime = java.time.LocalDateTime.parse(sendTimeStr);
            } catch (Exception e) {
                log.warn("sendTime解析失败: {}", sendTimeStr, e);
            }
        }

        if (sendTime != null && sendTime.isAfter(java.time.LocalDateTime.now())) {
            // 查找匹配的启用模板获取templateId（多租户隔离）
            Long tenantId = BaseContext.getCurrentTenantId();
            NotificationTemplate template = templateService.findByBizTypeAndTenant(bizType, tenantId);
            if (template == null) {
                return R.error("未找到业务类型[" + bizType + "]的启用模板");
            }
            NotificationRecord record = notificationService.batchSend(
                    template.getId(), targets, channel, 1, params, sendTime);
            return R.success(record);
        }

        NotificationRecord record = notificationService.sendByBizType(bizType, targets, channel, params);
        if (record != null) {
            return R.success(record);
        }
        return R.error("通知发送失败，请检查模板配置");
    }

    /**
     * 批量发送通知
     * @param body 发送参数（templateId/channel/targetType/targets/params/sendTime）
     * @return 发送记录
     */
    @PostMapping("/batch-send")
    @Operation(summary = "批量发送通知", description = "批量发送通知消息")
    @RequiresPermission("notification:send")
    @RateLimit(maxRequestsPerSecond = 3, type = RateLimitType.USER)
    public R<NotificationRecord> batchSend(
            @Parameter(description = "发送参数（templateId/channel/targetType/targets/params/sendTime）", required = true) @Valid @RequestBody BatchSendNotificationDTO dto) {
        Long templateId = dto.getTemplateId();
        Integer channel = dto.getChannel();
        Integer targetType = dto.getTargetType();
        List<String> targets = dto.getTargets();
        Map<String, String> params = dto.getParams();
        String sendTimeStr = dto.getSendTime();

        // 入口校验：targets 非空且不超上限，防止 NPE 与下游过载
        if (targets == null || targets.isEmpty()) {
            return R.error("目标用户列表不能为空");
        }
        if (targets.size() > MAX_BATCH_TARGETS) {
            return R.error("单次批量发送目标数量不能超过" + MAX_BATCH_TARGETS + "条");
        }

        java.time.LocalDateTime sendTime = null;
        if (sendTimeStr != null && !sendTimeStr.isEmpty()) {
            try {
                sendTime = java.time.LocalDateTime.parse(sendTimeStr);
            } catch (Exception e) {
                log.warn("sendTime解析失败: {}", sendTimeStr, e);
                return R.error("定时发送时间格式非法");
            }
        }

        NotificationRecord record = notificationService.batchSend(
                templateId, targets, channel, targetType, params, sendTime);
        return R.success(record);
    }

    /**
     * 向全部用户发送通知
     * @param body 推送参数（bizType/channel/params）
     * @return 发送记录
     */
    @PostMapping("/send-all")
    @Operation(summary = "全量推送", description = "向所有用户发送通知")
    @RequiresPermission("notification:send")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.GLOBAL)
    public R<NotificationRecord> sendToAllUsers(
            @Parameter(description = "推送参数（bizType/channel/params）", required = true) @Valid @RequestBody SendToAllUsersDTO dto) {
        String bizType = dto.getBizType();
        Integer channel = dto.getChannel();
        Map<String, String> params = dto.getParams();

        if (bizType == null) {
            return R.error("业务类型不能为空");
        }

        NotificationRecord record = notificationService.sendToAllUsers(bizType, channel, params);
        if (record != null) {
            return R.success(record);
        }
        return R.error("全量推送失败，请检查模板配置和用户数据");
    }

    // ==================== 发送记录 ====================

    /**
     * 分页查询发送记录
     */
    @GetMapping("/record/page")
    @Operation(summary = "分页查询发送记录", description = "分页查询通知发送记录")
    public R<Page<NotificationRecord>> recordPage(
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "业务类型")
            @RequestParam(required = false) String bizType,
            @Parameter(description = "状态")
            @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        // 域4 改造：分页查询下沉到 Service
        Page<NotificationRecord> pageInfo = recordService.pageRecords(page, pageSize, bizType, status, tenantId);
        return R.success(pageInfo);
    }

    /**
     * 发送记录今日统计
     * <p>使用 SQL 聚合替代前端 pageSize:999 拉全量后 forEach 计算，避免全表扫描</p>
     *
     * @return 今日短信数、推送数、成功数、失败数
     */
    @GetMapping("/record/stats")
    @Operation(summary = "发送记录今日统计", description = "聚合统计当日各渠道发送次数及成功/失败数")
    public R<Map<String, Object>> recordStats() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime start = today.atStartOfDay();
        java.time.LocalDateTime end = today.atTime(java.time.LocalTime.MAX);
        Long tenantId = BaseContext.getCurrentTenantId();
        // 域4 改造：聚合统计下沉到 Service
        Map<String, Object> stats = recordService.statBetween(start, end, tenantId);
        if (stats == null) {
            stats = new HashMap<>();
        }
        return R.success(stats);
    }

    /**
     * 获取发送记录详情
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/record/{id}")
    @Operation(summary = "查询发送记录详情", description = "根据ID查询通知发送记录详情")
    public R<NotificationRecord> recordDetail(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> checkResult = recordService.getRecordWithTenantCheck(id, tenantId);
        if (Boolean.FALSE.equals(checkResult.get("ok"))) {
            return R.error((String) checkResult.get("message"));
        }
        return R.success((NotificationRecord) checkResult.get("record"));
    }

    // ==================== 设备注册 ====================

    /**
     * 注册/更新用户设备Token
     * @param body 设备信息（userId/platform/deviceToken）
     * @return 操作结果
     */
    @PostMapping("/device/register")
    @Operation(summary = "注册设备Token", description = "注册/更新用户设备推送Token")
    public R<String> registerDevice(
            @Parameter(description = "设备信息（platform/deviceToken）", required = true) @Valid @RequestBody RegisterDeviceDTO dto) {
        // 修改点：以登录会话身份为准绑定设备，杜绝伪造他人 userId 的越权注册（IDOR）
        Long sessionUserId = BaseContext.getCurrentId();
        if (sessionUserId == null) {
            return R.error("请先登录");
        }
        String platform = dto.getPlatform();
        String deviceToken = dto.getDeviceToken();
        if (platform == null || platform.trim().isEmpty() || deviceToken == null || deviceToken.trim().isEmpty()) {
            return R.error("平台与设备Token不能为空");
        }

        notificationService.registerDevice(sessionUserId, platform, deviceToken);
        return R.success("设备注册成功");
    }

    /**
     * 获取业务类型枚举列表
     * @return 业务类型列表
     */
    @GetMapping("/biz-types")
    @Operation(summary = "业务类型列表", description = "获取所有通知业务类型枚举列表")
    public R<List<Map<String, String>>> getBizTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(buildBizType("ORDER_NOTICE", "订单通知"));
        types.add(buildBizType("PROMOTION", "营销推送"));
        types.add(buildBizType("VERIFY_CODE", "验证码"));
        types.add(buildBizType("SYSTEM", "系统通知"));
        types.add(buildBizType("JOB_NOTICE", "工作通知"));
        return R.success(types);
    }

    private Map<String, String> buildBizType(String code, String name) {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("name", name);
        return map;
    }

    // ==================== 简易消息发送 ====================

    /**
     * 简易消息发送（无需模板，直接输入内容发送）
     * @param body 消息参数（channel/targets/content/title）
     * @return 发送记录
     */
    @PostMapping("/send-simple")
    @Operation(summary = "简易消息发送", description = "直接发送消息内容，无需模板（支持推送/短信/站内信）")
    @RequiresPermission("notification:send")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    public R<NotificationRecord> sendSimpleMessage(
            @Parameter(description = "消息参数（channel/targets/content/title）", required = true) @Valid @RequestBody SendSimpleMessageDTO dto) {
        Integer channel = dto.getChannel();
        List<String> targets = dto.getTargets();
        String content = dto.getContent();
        String title = dto.getTitle();

        if (targets == null || targets.isEmpty()) {
            return R.error("目标用户不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return R.error("消息内容不能为空");
        }

        NotificationRecord record = notificationService.sendSimpleMessage(channel, targets, title, content);
        if (record != null) {
            return R.success(record);
        }
        return R.error("消息发送失败");
    }

    /**
     * 通知服务健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "通知服务健康检查", description = "检查通知服务是否可用")
    public R<Map<String, Object>> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("available", true);
        info.put("mockMode", notificationService.isMockMode());
        return R.success(info);
    }
}



