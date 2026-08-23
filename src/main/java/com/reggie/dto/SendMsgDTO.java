package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送短信验证码请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Schema(description = "发送短信验证码请求参数")
public class SendMsgDTO {

    @Schema(description = "手机号", required = true, example = "13800138000")
    private String phone;
}
