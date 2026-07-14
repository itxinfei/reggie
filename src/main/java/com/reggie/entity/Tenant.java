package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reggie.common.SecurityConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户实体
 */
@Data
@TableName("tenant")
@Schema(description = "租户")
public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "租户ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户名称", example = "瑞吉外卖总店", required = true)
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 64, message = "租户名称不能超过64个字符")
    private String name;

    @Schema(description = "手机号", example = "13800138000", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    @Schema(description = "地址", example = "北京市朝阳区xxx")
    @Size(max = 255, message = "地址不能超过255个字符")
    private String address;

    @Schema(description = "租户状态：0=禁用，1=正常", example = "1", required = true)
    @NotNull(message = "租户状态不能为空")
    private Integer status;

    @Schema(description = "密码加密类型", example = "MD5")
    private String passwordType = SecurityConstants.PASSWORD_TYPE_MD5;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
