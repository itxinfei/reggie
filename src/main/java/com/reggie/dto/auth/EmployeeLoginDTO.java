package com.reggie.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * <p>
 * 员工登录DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "员工登录信息")
public class EmployeeLoginDTO {

    @Schema(description = "用户名", required = true)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    @Schema(description = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;
}
