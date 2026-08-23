package com.reggie.module.notification.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 全量推送请求 DTO
 */
public class SendToAllUsersDTO {

    @NotNull(message = "业务类型不能为空")
    @Size(min = 1, max = 64, message = "业务类型长度必须在1-64之间")
    private String bizType;

    private Integer channel = 1;

    private Map<String, String> params;

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

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
