package com.reggie.module.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.reggie.common.BaseContext;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.mapper.UserDeviceMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.model.UserDevice;
import com.reggie.module.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息通知服务实现
 * 支持短信(阿里云)和APP推送(预留SDK接口)，具有完善的模板参数填充和失败处理
 *
 * @author Reggie Team
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    /** JSON序列化工具 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private NotificationTemplateMapper templateMapper;

    @Resource
    private NotificationRecordMapper recordMapper;

    @Resource
    private UserDeviceMapper userDeviceMapper;

    /** 模板占位符正则: ${paramName} */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

    // 阿里云短信配置（开发环境屏蔽真实发送，仅记录日志）
    private static final boolean SMS_MOCK_MODE = true;
    private static final String SMS_REGION = "cn-hangzhou";
    private static final String SMS_ACCESS_KEY = "";
    private static final String SMS_SECRET_KEY = "";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord sendByBizType(String bizType, List<String> targets, Integer channel,
                                            Map<String, String> params) {
        if (bizType == null || targets == null || targets.isEmpty()) {
            log.warn("sendByBizType参数无效: bizType={}, targets={}", bizType, targets);
            return null;
        }

        // 查找匹配的启用模板
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getBizType, bizType)
               .eq(NotificationTemplate::getStatus, 1)
               .last("LIMIT 1");
        NotificationTemplate template = templateMapper.selectOne(wrapper);
        if (template == null) {
            log.warn("未找到业务类型[{}]的启用模板", bizType);
            return null;
        }

        // 渲染模板内容
        String content = renderTemplate(template.getContent(), params);
        String title = template.getTitle() != null
                ? renderTemplate(template.getTitle(), params) : null;

        // 创建发送记录
        NotificationRecord record = buildRecord(template.getId(), bizType, channel,
                targets, content, null);
        recordMapper.insert(record);

        // 执行发送
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (int i = 0; i < targets.size(); i++) {
            try {
                boolean ok = false;
                if (channel == 1) {
                    ok = sendSms(targets.get(i), template.getSignName(),
                            template.getTemplateCode(), buildSmsParam(params));
                } else if (channel == 2) {
                    ok = sendPushToUser(targets.get(i), title, content);
                }
                if (ok) {
                    successCount++;
                } else {
                    failCount++;
                    failReasons.append("[").append(targets.get(i)).append("]发送失败; ");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.append("[").append(targets.get(i)).append("]异常:")
                        .append(e.getMessage()).append("; ");
                log.error("通知发送异常: target={}, channel={}", targets.get(i), channel, e);
            }
        }

        // 更新记录状态
        updateRecordResult(record, successCount, failCount, failReasons.toString());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord batchSend(Long templateId, List<String> targets, Integer channel,
                                        Integer targetType, Map<String, String> params,
                                        LocalDateTime sendTime) {
        NotificationTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        String content = renderTemplate(template.getContent(), params);
        String title = template.getTitle() != null
                ? renderTemplate(template.getTitle(), params) : null;

        NotificationRecord record = buildRecord(templateId, template.getBizType(), channel,
                targets, content, sendTime);
        record.setTargetType(targetType != null ? targetType : 1);
        recordMapper.insert(record);

        // 定时发送暂不立即执行，交由调度任务处理
        if (sendTime != null && sendTime.isAfter(LocalDateTime.now())) {
            log.info("通知已加入定时发送队列: recordId={}, sendTime={}", record.getId(), sendTime);
            return record;
        }

        // 立即发送
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (String target : targets) {
            try {
                boolean ok = false;
                if (channel == 1) {
                    ok = sendSms(target, template.getSignName(), template.getTemplateCode(),
                            buildSmsParam(params));
                } else if (channel == 2) {
                    ok = sendPushToUser(target, title, content);
                }
                if (ok) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                failReasons.append("[").append(target).append("]").append(e.getMessage()).append("; ");
                log.error("批量发送异常: target={}", target, e);
            }
        }

        updateRecordResult(record, successCount, failCount, failReasons.toString());
        return record;
    }

    @Override
    public boolean sendSms(String phone, String signName, String templateCode, String params) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        if (SMS_MOCK_MODE) {
            log.info("[短信Mock] phone={}, sign={}, template={}, params={}",
                    phone, signName, templateCode, params);
            return true;
        }

        try {
            DefaultProfile profile = DefaultProfile.getProfile(SMS_REGION, SMS_ACCESS_KEY, SMS_SECRET_KEY);
            IAcsClient client = new DefaultAcsClient(profile);

            SendSmsRequest request = new SendSmsRequest();
            request.setSysRegionId(SMS_REGION);
            request.setPhoneNumbers(phone);
            request.setSignName(signName);
            request.setTemplateCode(templateCode);
            request.setTemplateParam(params);

            SendSmsResponse response = client.getAcsResponse(request);
            if ("OK".equals(response.getCode())) {
                log.info("短信发送成功: phone={}, bizId={}", phone, response.getBizId());
                return true;
            } else {
                log.warn("短信发送失败: phone={}, code={}, msg={}",
                        phone, response.getCode(), response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phone, e);
            return false;
        }
    }

    @Override
    public int sendAppPush(List<Long> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        // 查询用户设备Token
        LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserDevice::getUserId, userIds)
               .eq(UserDevice::getPushEnabled, 1)
               .isNotNull(UserDevice::getDeviceToken);
        List<UserDevice> devices = userDeviceMapper.selectList(wrapper);

        int successCount = 0;
        for (UserDevice device : devices) {
            try {
                // 修改点：APP推送mock实现，生产环境对接极光推送/个推SDK
                log.info("[APP推送Mock] userId={}, platform={}, token={}, title={}, content={}",
                        device.getUserId(), device.getPlatform(),
                        maskToken(device.getDeviceToken()), title, content);
                successCount++;
            } catch (Exception e) {
                log.error("APP推送异常: userId={}", device.getUserId(), e);
            }
        }

        log.info("APP推送完成: 目标{}人, 成功{}人", userIds.size(), successCount);
        return successCount;
    }

    @Override
    public void registerDevice(Long userId, String platform, String deviceToken) {
        if (userId == null || platform == null) {
            return;
        }

        LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDevice::getUserId, userId)
               .eq(UserDevice::getPlatform, platform.toUpperCase());
        UserDevice existing = userDeviceMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setDeviceToken(deviceToken);
            existing.setLastActiveTime(LocalDateTime.now());
            userDeviceMapper.updateById(existing);
        } else {
            UserDevice device = new UserDevice();
            device.setUserId(userId);
            device.setPlatform(platform.toUpperCase());
            device.setDeviceToken(deviceToken);
            device.setPushEnabled(1);
            device.setLastActiveTime(LocalDateTime.now());
            userDeviceMapper.insert(device);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 渲染模板，替换 ${param} 占位符
     */
    private String renderTemplate(String template, Map<String, String> params) {
        if (template == null || params == null) {
            return template;
        }
        String result = template;
        Matcher matcher = TEMPLATE_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String value = params.getOrDefault(paramName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 构建阿里云短信参数JSON
     */
    private String buildSmsParam(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    /**
     * 模拟推送到单个用户
     */
    private boolean sendPushToUser(String userIdStr, String title, String content) {
        try {
            Long userId = Long.parseLong(userIdStr);
            LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserDevice::getUserId, userId)
                   .eq(UserDevice::getPushEnabled, 1)
                   .isNotNull(UserDevice::getDeviceToken)
                   .last("LIMIT 1");
            UserDevice device = userDeviceMapper.selectOne(wrapper);
            if (device != null) {
                log.info("[APP推送Mock] userId={}, platform={}, title={}, content={}",
                        userId, device.getPlatform(), title, content);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userIdStr);
            return false;
        }
    }

    /**
     * 构建通知记录对象
     */
    private NotificationRecord buildRecord(Long templateId, String bizType, Integer channel,
                                           List<String> targets, String content,
                                           LocalDateTime sendTime) {
        NotificationRecord record = new NotificationRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setTemplateId(templateId);
        record.setBizType(bizType);
        record.setChannel(channel != null ? channel : 1);
        record.setTargetType(1);
        try {
            record.setTargetValue(objectMapper.writeValueAsString(targets));
        } catch (Exception e) {
            log.error("目标列表JSON序列化失败", e);
            record.setTargetValue("[]");
        }
        record.setTargetCount(targets.size());
        record.setContent(content);
        record.setSendTime(sendTime);
        record.setStatus(0);
        record.setSuccessCount(0);
        record.setFailCount(0);
        return record;
    }

    /**
     * 更新发送记录结果
     */
    private void updateRecordResult(NotificationRecord record, int successCount,
                                    int failCount, String failReason) {
        record.setSuccessCount(successCount);
        record.setFailCount(failCount);
        record.setFailReason(failReason);
        if (failCount == 0) {
            record.setStatus(2);
        } else if (successCount == 0) {
            record.setStatus(3);
        } else {
            record.setStatus(4);
        }
        recordMapper.updateById(record);
    }

    /**
     * 脱敏Token用于日志输出
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
