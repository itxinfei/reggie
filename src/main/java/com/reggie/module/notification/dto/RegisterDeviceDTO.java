package com.reggie.module.notification.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 注册设备 Token 请求 DTO
 */
public class RegisterDeviceDTO {

    @NotBlank(message = "平台不能为空")
    @Size(max = 32, message = "平台长度不能超过32")
    private String platform;

    @NotBlank(message = "设备Token不能为空")
    @Size(max = 512, message = "设备Token长度不能超过512")
    private String deviceToken;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
