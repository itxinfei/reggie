package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class UserLoginDTO {

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(description = "验证码", required = true, example = "1234")
    @NotBlank(message = "验证码不能为空")
    private String code;
}
