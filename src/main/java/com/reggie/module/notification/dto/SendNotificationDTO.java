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

    /**
     * 获取 biz type。
     * @return 返回结果
     */
    public String getBizType() {
        return bizType;
    }

    /**
     * 设置 biz type。
     * @param bizType 参数 bizType
     */
    public void setBizType(String bizType) {
        this.bizType = bizType;
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
