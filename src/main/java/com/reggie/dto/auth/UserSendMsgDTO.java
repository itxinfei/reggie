package com.reggie.dto.auth;

import com.reggie.common.SecurityConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户发送短信DTO
 */
@Data
@Schema(description = "用户手机号信息")
public class UserSendMsgDTO {

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;
}
