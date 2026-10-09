package com.reggie.module.notification.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 批量发送通知请求 DTO
 */
public class BatchSendNotificationDTO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    private Integer channel = 1;

    private Integer targetType = 1;

    @NotEmpty(message = "目标用户列表不能为空")
    private List<String> targets;

    private Map<String, String> params;

    @Size(max = 64, message = "发送时间长度不能超过64")
    private String sendTime;

    /**
     * 获取 template id。
     * @return 返回结果
     */
    public Long getTemplateId() {
        return templateId;
    }

    /**
     * 设置 template id。
     * @param templateId 参数 templateId
     */
    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    /**
     * 获取 channel。
     * @return 返回结果
     */
    public Integer getChannel() {
        return channel;
    }

    /**
     * 设置 channel。
     * @param channel 参数 channel
     */
    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    /**
     * 获取 target type。
     * @return 返回结果
     */
    public Integer getTargetType() {
        return targetType;
    }

    /**
     * 设置 target type。
     * @param targetType 参数 targetType
     */
    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取 targets。
     * @return 返回结果
     */
    public List<String> getTargets() {
        return targets;
    }

    /**
     * 设置 targets。
     * @param targets 参数 targets
     */
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    /**
     * 获取 params。
     * @return 返回结果
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * 设置 params。
     * @param params 参数 params
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    /**
     * 获取 send time。
     * @return 返回结果
     */
    public String getSendTime() {
        return sendTime;
    }

    /**
     * 设置 send time。
     * @param sendTime 参数 sendTime
     */
    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }
}
