package com.reggie.module.notification.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 发送通知请求 DTO
 */
public class SendNotificationDTO {

    @NotNull(message = "业务类型不能为空")
    @Size(min = 1, max = 64, message = "业务类型长度必须在1-64之间")
    private String bizType;

    private Integer channel = 1;

    @NotEmpty(message = "目标用户不能为空")
    private List<String> targets;

    private Map<String, String> params;

    @Size(max = 64, message = "发送时间长度不能超过64")
    private String sendTime;

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
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
