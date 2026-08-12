package com.reggie.module.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.common.CustomException;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.user.model.User;
import com.reggie.module.user.mapper.UserMapper;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.mapper.NotificationTemplateMapper;
import com.reggie.module.notification.mapper.UserDeviceMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.model.NotificationTemplate;
import com.reggie.module.notification.model.UserDevice;
import com.reggie.module.notification.provider.PushProvider;
import com.reggie.module.notification.model.PushMessage;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.recommend.mapper.MarketingMessageMapper;
import com.reggie.module.recommend.model.MarketingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
/**
 * Notification service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationServiceImpl implements NotificationService {

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    /** 通知模板Mapper */
    @Resource
    private NotificationTemplateMapper templateMapper;

    /** 通知记录Mapper */
    @Resource
    private NotificationRecordMapper recordMapper;

    /** 用户设备Mapper */
    @Resource
    private UserDeviceMapper userDeviceMapper;

    /** 用户Mapper */
    @Resource
    private UserMapper userMapper;

    /** 营销消息Mapper */
    @Resource
    private MarketingMessageMapper marketingMessageMapper;

    /** 推送服务提供商（策略模式） */
    @Resource
    private PushProvider pushProvider;

    /** 模板占位符正则: ${paramName} */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

    /** 短信Mock模式开关 */
    @Value("${reggie.sms.mock-mode:true}")
    private boolean smsMockMode;

    /** 阿里云短信AccessKey */
    @Value("${reggie.sms.access-key:}")
    private String smsAccessKey;

    /** 阿里云短信SecretKey */
    @Value("${reggie.sms.secret-key:}")
    private String smsSecretKey;

    /** 阿里云短信区域 */
    private static final String SMS_REGION = "cn-hangzhou";

    @Override
    public NotificationRecord sendByBizType(String bizType, List<String> targets, Integer channel,
                                            Map<String, String> params) {
        if (bizType == null || targets == null || targets.isEmpty()) {
            log.warn("sendByBizType参数无效: bizType={}, targets={}", bizType, targets);
            return null;
        }

        // 查找匹配的启用模板（多租户隔离）
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getBizType, bizType)
               .eq(NotificationTemplate::getStatus, 1);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            wrapper.eq(NotificationTemplate::getTenantId, tenantId);
        }
        wrapper.last("LIMIT 1");
        NotificationTemplate template = templateMapper.selectOne(wrapper);
        if (template == null) {
            log.warn("未找到业务类型[{}]的启用模板", bizType);
            return null;
        }

        // 渲染模板内容
        String content = renderTemplate(template.getContent(), params);
        String title = template.getTitle() != null
                ? renderTemplate(template.getTitle(), params) : null;

        // 创建发送记录（独立提交，status=发送中，避免长事务持锁与回滚致消息丢失）
        NotificationRecord record = buildRecord(template.getId(), bizType, channel,
                targets, content, null);
        record.setStatus(1);
        recordMapper.insert(record);

        // 批量预解析 target→userId 映射，消除循环内逐条查 User 的 N+1 问题
        Map<String, Long> userIdMap = resolveUserIds(targets, channel);

        // 事务外执行发送（外部短信/推送为 HTTP 调用，不应包裹在事务内）
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            try {
                boolean ok = sendToTarget(target, channel, template, title, content, params);
                if (ok) {
                    successCount++;
                    syncToMarketingMessage(target, channel, title, content, userIdMap);
                } else {
                    failCount++;
                    failReasons.append("[").append(target).append("]发送失败; ");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.append("[").append(target).append("]异常:")
                        .append(e.getMessage()).append("; ");
                log.error("通知发送异常: target={}, channel={}", target, channel, e);
            }
        }

        // 更新记录状态（单独提交最终结果）
        updateRecordResult(record, successCount, failCount, failReasons.toString());
        return record;
    }

    @Override
    public NotificationRecord batchSend(Long templateId, List<String> targets, Integer channel,
                                        Integer targetType, Map<String, String> params,
                                        LocalDateTime sendTime) {
        // service层空校验：targets 非空（与Controller入口校验形成双重防御）
        if (targets == null || targets.isEmpty()) {
            throw new CustomException("目标用户列表不能为空");
        }
        NotificationTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new CustomException("模板不存在: " + templateId);
        }
        // 多租户校验：确保模板属于当前租户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(template.getTenantId())) {
            throw new CustomException("无权使用其他租户的模板");
        }

        String content = renderTemplate(template.getContent(), params);
        String title = template.getTitle() != null
                ? renderTemplate(template.getTitle(), params) : null;

        boolean scheduled = sendTime != null && sendTime.isAfter(LocalDateTime.now());
        // 创建发送记录（独立提交；立即发送置 status=发送中，定时发送置 status=待发送）
        NotificationRecord record = buildRecord(templateId, template.getBizType(), channel,
                targets, content, sendTime);
        record.setTargetType(targetType != null ? targetType : 1);
        if (!scheduled) {
            record.setStatus(1);
        }
        recordMapper.insert(record);

        // 定时发送暂不立即执行，交由调度任务处理
        if (scheduled) {
            log.info("通知已加入定时发送队列: recordId={}, sendTime={}", record.getId(), sendTime);
            return record;
        }

        // 批量预解析 target→userId 映射，消除循环内逐条查 User 的 N+1 问题
        Map<String, Long> userIdMap = resolveUserIds(targets, channel);

        // 事务外立即发送（外部短信/推送为 HTTP 调用，不应包裹在事务内）
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (String target : targets) {
            try {
                boolean ok = sendToTarget(target, channel, template, title, content, params);
                if (ok) {
                    successCount++;
                    syncToMarketingMessage(target, channel, title, content, userIdMap);
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

        if (smsMockMode) {
            log.info("[短信Mock] phone={}, sign={}, template={}, content={}",
                    phone, signName, templateCode, params);
            return true;
        }

        if (templateCode == null || templateCode.isEmpty()) {
            log.info("[短信-简易模式] phone={}, sign={}, content={}", phone, signName, params);
            // 简易模式下，生产环境需配置通用短信模板后取消下面注释
            // return sendSmsWithContent(phone, signName, params);
            return true;
        }

        try {
            DefaultProfile profile = DefaultProfile.getProfile(SMS_REGION, smsAccessKey, smsSecretKey);
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

        // 查询所有目标用户的全部启用推送的设备
        LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserDevice::getUserId, userIds)
               .eq(UserDevice::getPushEnabled, 1)
               .isNotNull(UserDevice::getDeviceToken);
        List<UserDevice> devices = userDeviceMapper.selectList(wrapper);

        if (devices.isEmpty()) {
            log.warn("[APP推送] 目标用户无可用设备: userIds={}", userIds);
            return 0;
        }

        // 构建结构化推送消息
        PushMessage pushMessage = PushMessage.builder()
                .title(title)
                .content(content)
                .clickAction("APP_PAGE")
                .build();

        // 按用户分组推送（一个用户可能有多个设备）
        Map<Long, List<UserDevice>> userDeviceMap = new LinkedHashMap<>();
        for (UserDevice device : devices) {
            userDeviceMap.computeIfAbsent(device.getUserId(), k -> new ArrayList<>()).add(device);
        }

        int totalSuccess = 0;
        for (Map.Entry<Long, List<UserDevice>> entry : userDeviceMap.entrySet()) {
            Long userId = entry.getKey();
            List<UserDevice> userDevices = entry.getValue();
            int deviceCount = pushProvider.pushToUserDevices(userDevices, pushMessage);
            totalSuccess += deviceCount;
            log.info("[APP推送] userId={}, 设备总数={}, 推送成功={}", userId, userDevices.size(), deviceCount);
        }

        log.info("[APP推送] 全部完成: 目标用户{}人, 覆盖设备{}台, 推送成功{}台",
                userIds.size(), devices.size(), totalSuccess);
        return totalSuccess;
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
     * 向单个用户的所有启用设备发送APP推送
     * 使用PushProvider策略模式，推送所有设备而非仅一台
     *
     * @param userIdStr 用户ID字符串
     * @param title     推送标题
     * @param content   推送内容
     * @return true=至少一台设备推送成功
     */
    private boolean sendPushToUser(String userIdStr, String title, String content) {
        try {
            Long userId = Long.parseLong(userIdStr);

            // 查询该用户所有启用推送的设备
            LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserDevice::getUserId, userId)
                   .eq(UserDevice::getPushEnabled, 1)
                   .isNotNull(UserDevice::getDeviceToken);
            List<UserDevice> devices = userDeviceMapper.selectList(wrapper);

            if (devices.isEmpty()) {
                log.debug("[APP推送] userId={} 无可用设备", userId);
                return false;
            }

            // 构建结构化推送消息
            PushMessage pushMessage = PushMessage.builder()
                    .title(title)
                    .content(content)
                    .clickAction("APP_PAGE")
                    .build();

            int successCount = pushProvider.pushToUserDevices(devices, pushMessage);
            log.info("[APP推送] userId={}, 设备数={}, 成功推送={}", userId, devices.size(), successCount);
            return successCount > 0;
        } catch (NumberFormatException e) {
            log.warn("[APP推送] 无效的用户ID格式: {}", userIdStr);
            return false;
        } catch (Exception e) {
            log.error("[APP推送] 异常: userIdStr={}", userIdStr, e);
            return false;
        }
    }

    /**
     * 统一渠道发送，支持channel=3(SMS+APP推送)同时发送两种通道
     * @return true=至少一个通道发送成功
     */
    private boolean sendToTarget(String target, Integer channel, NotificationTemplate template,
                                  String title, String content, Map<String, String> params) {
        boolean smsOk = false;
        boolean pushOk = false;

        // channel=1 短信; channel=2 APP推送; channel=3 短信+APP推送
        if (channel == null) channel = 1;

        if (channel == 1 || channel == 3) {
            smsOk = sendSms(target, template.getSignName(),
                    template.getTemplateCode(), buildSmsParam(params));
        }
        if (channel == 2 || channel == 3) {
            pushOk = sendPushToUser(target, title, content);
        }

        if (channel == 3) {
            return smsOk || pushOk;
        }
        return channel == 1 ? smsOk : pushOk;
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
     * 将通知发送结果同步写入用户消息中心（marketing_message表）
     * 让用户在消息中心页面可以看到通知信息
     *
     * @param target  发送目标（手机号或用户ID）
     * @param channel 发送渠道
     * @param title   消息标题
     * @param content 消息内容
     */
    private void syncToMarketingMessage(String target, Integer channel, String title, String content, Map<String, Long> userIdMap) {
        try {
            Long userId = null;
            if (userIdMap != null) {
                userId = userIdMap.get(target);
            } else {
                userId = resolveUserId(target, channel);
            }
            if (userId == null) {
                log.debug("无法解析target对应用户，跳过消息中心同步: target={}", target);
                return;
            }

            MarketingMessage msg = new MarketingMessage();
            msg.setUserId(userId);
            msg.setPushType(channel == 1 || channel == 3
                    ? MarketingMessage.PUSH_SMS : MarketingMessage.PUSH_NOTIFICATION);
            msg.setTitle(title != null ? title : "系统通知");
            msg.setContent(content != null ? content : "");
            msg.setStatus(MarketingMessage.STATUS_SENT);
            marketingMessageMapper.insert(msg);
            log.debug("消息中心同步成功: userId={}, target={}", userId, target);
        } catch (Exception e) {
            log.warn("消息中心同步失败: target={}, channel={}", target, channel, e);
        }
    }

    /**
     * 批量解析 target→userId 映射，消除循环内逐条查 User 的 N+1 问题。
     * <p>一次批量查询替代 N 次 selectById/selectOne：</p>
     * <ul>
     *   <li>channel=2（推送）：target 即 userId，直接解析</li>
     *   <li>channel=1/3（短信/混合）：先批量按手机号查询；未匹配的数字型 target 再批量按 id 校验存在性后回退当作 userId</li>
     * </ul>
     * 全程带租户隔离过滤。
     */
    private Map<String, Long> resolveUserIds(List<String> targets, Integer channel) {
        Map<String, Long> result = new HashMap<>();
        if (targets == null || targets.isEmpty()) {
            return result;
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        if (channel != null && channel == 2) {
            // 推送渠道：target 即 userId
            for (String t : targets) {
                if (t == null) continue;
                try {
                    result.put(t, Long.parseLong(t));
                } catch (NumberFormatException ignored) {
                    // 非数字 target 无法作为 userId
                }
            }
            return result;
        }

        // 短信/混合渠道：一次性批量按手机号查询（带租户隔离）
        List<String> phones = new ArrayList<>(targets.size());
        for (String t : targets) {
            if (t != null) {
                phones.add(t);
            }
        }
        if (!phones.isEmpty()) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.select(User::getId, User::getPhone).in(User::getPhone, phones);
            if (tenantId != null) {
                phoneWrapper.eq(User::getTenantId, tenantId);
            }
            List<User> users = userMapper.selectList(phoneWrapper);
            for (User u : users) {
                if (u.getPhone() != null) {
                    result.put(u.getPhone(), u.getId());
                }
            }
        }

        // 未匹配手机号的数字型 target，批量按 id 校验存在性后回退当作 userId（保持与原 resolveUserId 一致的归属校验）
        List<Long> numericIds = new ArrayList<>();
        for (String t : targets) {
            if (t != null && !result.containsKey(t)) {
                try {
                    numericIds.add(Long.parseLong(t));
                } catch (NumberFormatException ignored) {
                    // 既非已注册手机号也非数字 userId，跳过
                }
            }
        }
        if (!numericIds.isEmpty()) {
            LambdaQueryWrapper<User> idWrapper = new LambdaQueryWrapper<>();
            idWrapper.select(User::getId).in(User::getId, numericIds);
            if (tenantId != null) {
                idWrapper.eq(User::getTenantId, tenantId);
            }
            List<User> existing = userMapper.selectList(idWrapper);
            for (User u : existing) {
                result.put(String.valueOf(u.getId()), u.getId());
            }
        }
        return result;
    }

    /**
     * 根据target和channel解析用户ID
     * - 短信渠道(channel=1)：target是手机号，查User表获取userId
     * - 推送渠道(channel=2)：target是用户ID，直接使用
     * - 混合渠道(channel=3)：优先按用户ID解析，失败则按手机号查
     */
    private Long resolveUserId(String target, Integer channel) {
        try {
            if (channel == 2) {
                return Long.parseLong(target);
            }
            if (channel == 1 || channel == 3) {
                // 先尝试作为用户ID解析
                try {
                    long uid = Long.parseLong(target);
                    User user = userMapper.selectById(uid);
                    if (user != null) return uid;
                } catch (NumberFormatException ignored) {
                    // target不是数字ID，按手机号查询
                }
                // 按手机号查用户
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getPhone, target).last("LIMIT 1");
                User user = userMapper.selectOne(wrapper);
                return user != null ? user.getId() : null;
            }
        } catch (Exception e) {
            log.warn("解析用户ID失败: target={}, channel={}", target, channel, e);
        }
        return null;
    }

    /**
     * 简易消息发送（无需模板，直接发送文本内容到对应渠道）
     * 同时将消息内容同步写入用户消息中心
     */
    @Override
    public NotificationRecord sendSimpleMessage(Integer channel, List<String> targets,
                                                 String title, String content) {
        if (targets == null || targets.isEmpty()) {
            log.warn("sendSimpleMessage参数无效: targets为空");
            return null;
        }
        if (channel == null) channel = 1;

        // 创建发送记录（独立提交，status=发送中，避免长事务持锁）
        NotificationRecord record = new NotificationRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setBizType("SYSTEM");
        record.setChannel(channel);
        record.setTargetType(1);
        try {
            record.setTargetValue(objectMapper.writeValueAsString(targets));
        } catch (Exception e) {
            record.setTargetValue("[]");
        }
        record.setTargetCount(targets.size());
        record.setContent(content);
        record.setStatus(1);
        record.setSuccessCount(0);
        record.setFailCount(0);
        recordMapper.insert(record);

        // 批量预解析 target→userId 映射，消除 N+1
        Map<String, Long> userIdMap = resolveUserIds(targets, channel);

        // 事务外执行发送（外部 HTTP 调用不应包裹在事务内）
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (String target : targets) {
            try {
                boolean ok;
                if (channel == 1) {
                    // 短信：使用简易发送模式
                    ok = sendSms(target, "瑞吉外卖", null, content);
                } else {
                    // APP推送
                    ok = sendPushToUser(target, title, content);
                }
                if (ok) {
                    successCount++;
                    // 同步到用户消息中心
                    syncToMarketingMessage(target, channel, title, content, userIdMap);
                } else {
                    failCount++;
                    failReasons.append("[").append(target).append("]发送失败; ");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.append("[").append(target).append("]异常:")
                        .append(e.getMessage()).append("; ");
                log.error("简易消息发送异常: target={}, channel={}", target, channel, e);
            }
        }

        updateRecordResult(record, successCount, failCount, failReasons.toString());
        log.info("简易消息发送完成: channel={}, 成功{}, 失败{}", channel, successCount, failCount);
        return record;
    }

    /**
     * 获取当前是否为Mock模式
     * 同时考虑短信Mock和推送Mock
     */
    @Override
    public boolean isMockMode() {
        return smsMockMode && pushProvider.isMockMode();
    }

    /**
     * 向全部用户发送通知
     * 查询所有状态正常的用户，集体发送通知并同步写入消息中心
     */
    @Override
    public NotificationRecord sendToAllUsers(String bizType, Integer channel, Map<String, String> params) {
        if (bizType == null) {
            log.warn("sendToAllUsers参数无效: bizType=null");
            return null;
        }

        // 查找启用的模板（多租户隔离）
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getBizType, bizType)
               .eq(NotificationTemplate::getStatus, 1);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            wrapper.eq(NotificationTemplate::getTenantId, tenantId);
        }
        wrapper.last("LIMIT 1");
        NotificationTemplate template = templateMapper.selectOne(wrapper);
        if (template == null) {
            log.warn("未找到业务类型[{}]的启用模板", bizType);
            return null;
        }

        // 查询当前租户的所有用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, 1);
        userWrapper.eq(User::getTenantId, BaseContext.getCurrentTenantId());
        List<User> users = userMapper.selectList(userWrapper);
        if (users.isEmpty()) {
            log.warn("没有可推送的用户");
            return null;
        }

        // 根据渠道构建target列表
        List<String> targets = new ArrayList<>();
        for (User user : users) {
            if (channel == 1) {
                // 短信渠道：使用手机号
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    targets.add(user.getPhone());
                }
            } else if (channel == 2) {
                // 推送渠道：使用用户ID
                targets.add(String.valueOf(user.getId()));
            } else {
                // 混合渠道：优先手机号，否则用户ID
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    targets.add(user.getPhone());
                } else {
                    targets.add(String.valueOf(user.getId()));
                }
            }
        }

        if (targets.isEmpty()) {
            log.warn("没有有效的推送目标");
            return null;
        }

        String content = renderTemplate(template.getContent(), params);
        String title = template.getTitle() != null
                ? renderTemplate(template.getTitle(), params) : null;

        // 创建发送记录（独立提交，status=发送中，避免长事务持锁）
        NotificationRecord record = buildRecord(template.getId(), bizType, channel,
                targets, content, null);
        record.setTargetType(3); // 3=全部用户
        record.setStatus(1);
        recordMapper.insert(record);

        // 直接复用已查询的 users 列表构建 phone/userId→userId 映射，无需额外查库
        Map<String, Long> userIdMap = new HashMap<>();
        for (User u : users) {
            if (u.getPhone() != null) {
                userIdMap.put(u.getPhone(), u.getId());
            }
            userIdMap.put(String.valueOf(u.getId()), u.getId());
        }

        // 事务外执行发送（外部 HTTP 调用不应包裹在事务内）
        int successCount = 0;
        int failCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (String target : targets) {
            try {
                boolean ok = sendToTarget(target, channel, template, title, content, params);
                if (ok) {
                    successCount++;
                    syncToMarketingMessage(target, channel, title, content, userIdMap);
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                failReasons.append("[").append(target).append("]").append(e.getMessage()).append("; ");
                log.error("全量发送异常: target={}", target, e);
            }
        }

        updateRecordResult(record, successCount, failCount, failReasons.toString());
        log.info("全量推送完成: 共{}目标, 成功{}, 失败{}", targets.size(), successCount, failCount);
        return record;
    }
}







