package com.reggie.module.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 消息通知管理Controller
 * 提供通知模板管理、消息发送、发送记录查询等接口
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private NotificationTemplateMapper templateMapper;

    @Resource
    private NotificationRecordMapper recordMapper;

    // ==================== 模板管理 ====================

    /**
     * 分页查询通知模板
     */
    @GetMapping("/template/page")
    public R<Page<NotificationTemplate>> templatePage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) String bizType) {
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
     * 获取模板详情
     */
    @GetMapping("/template/{id}")
    public R<NotificationTemplate> templateDetail(@PathVariable Long id) {
        NotificationTemplate template = templateMapper.selectById(id);
        if (template != null) {
            return R.success(template);
        }
        return R.error("模板不存在");
    }

    /**
     * 新增通知模板
     */
    @PostMapping("/template")
    public R<String> addTemplate(@RequestBody NotificationTemplate template) {
        templateMapper.insert(template);
        log.info("新增通知模板: id={}, name={}", template.getId(), template.getTemplateName());
        return R.success("添加成功");
    }

    /**
     * 修改通知模板
     */
    @PutMapping("/template")
    public R<String> updateTemplate(@RequestBody NotificationTemplate template) {
        templateMapper.updateById(template);
        log.info("更新通知模板: id={}", template.getId());
        return R.success("修改成功");
    }

    /**
     * 启用/停用模板
     */
    @PutMapping("/template/{id}/status/{status}")
    public R<String> toggleTemplate(@PathVariable Long id, @PathVariable Integer status) {
        NotificationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return R.error("模板不存在");
        }
        template.setStatus(status);
        templateMapper.updateById(template);
        return R.success(status == 1 ? "已启用" : "已停用");
    }

    /**
     * 删除通知模板
     */
    @DeleteMapping("/template/{id}")
    public R<String> deleteTemplate(@PathVariable Long id) {
        templateMapper.deleteById(id);
        log.info("删除通知模板: id={}", id);
        return R.success("删除成功");
    }

    // ==================== 消息发送 ====================

    /**
     * 发送通知（通用接口）
     * 请求体: { "bizType": "ORDER_NOTICE", "channel": 1, "targets": ["13800138000"],
     *          "params": { "userName": "张三", "orderNo": "NO123" } }
     */
    @PostMapping("/send")
    public R<NotificationRecord> sendNotification(@RequestBody Map<String, Object> body) {
        String bizType = (String) body.get("bizType");
        Integer channel = (Integer) body.getOrDefault("channel", 1);
        @SuppressWarnings("unchecked")
        List<String> targets = (List<String>) body.get("targets");
        @SuppressWarnings("unchecked")
        Map<String, String> params = (Map<String, String>) body.get("params");

        if (bizType == null || targets == null || targets.isEmpty()) {
            return R.error("业务类型和目标用户不能为空");
        }

        NotificationRecord record = notificationService.sendByBizType(bizType, targets, channel, params);
        if (record != null) {
            return R.success(record);
        }
        return R.error("通知发送失败，请检查模板配置");
    }

    /**
     * 批量发送通知
     */
    @PostMapping("/batch-send")
    public R<NotificationRecord> batchSend(@RequestBody Map<String, Object> body) {
        Long templateId = Long.valueOf(body.get("templateId").toString());
        Integer channel = (Integer) body.getOrDefault("channel", 1);
        Integer targetType = (Integer) body.getOrDefault("targetType", 1);
        @SuppressWarnings("unchecked")
        List<String> targets = (List<String>) body.get("targets");
        @SuppressWarnings("unchecked")
        Map<String, String> params = (Map<String, String>) body.get("params");
        String sendTimeStr = (String) body.get("sendTime");

        java.time.LocalDateTime sendTime = null;
        if (sendTimeStr != null && !sendTimeStr.isEmpty()) {
            sendTime = java.time.LocalDateTime.parse(sendTimeStr);
        }

        NotificationRecord record = notificationService.batchSend(
                templateId, targets, channel, targetType, params, sendTime);
        return R.success(record);
    }

    // ==================== 发送记录 ====================

    /**
     * 分页查询发送记录
     */
    @GetMapping("/record/page")
    public R<Page<NotificationRecord>> recordPage(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) String bizType,
                                                   @RequestParam(required = false) Integer status) {
        Page<NotificationRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<NotificationRecord> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationRecord::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(NotificationRecord::getStatus, status);
        }
        wrapper.orderByDesc(NotificationRecord::getCreateTime);
        recordMapper.selectPage(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 获取发送记录详情
     */
    @GetMapping("/record/{id}")
    public R<NotificationRecord> recordDetail(@PathVariable Long id) {
        NotificationRecord record = recordMapper.selectById(id);
        if (record != null) {
            return R.success(record);
        }
        return R.error("记录不存在");
    }

    // ==================== 设备注册 ====================

    /**
     * 注册/更新用户设备Token
     */
    @PostMapping("/device/register")
    public R<String> registerDevice(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String platform = (String) body.get("platform");
        String deviceToken = (String) body.get("deviceToken");

        notificationService.registerDevice(userId, platform, deviceToken);
        return R.success("设备注册成功");
    }

    /**
     * 获取业务类型枚举列表
     */
    @GetMapping("/biz-types")
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
}
