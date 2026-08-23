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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }
}
