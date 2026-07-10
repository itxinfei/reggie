package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reggie.common.SecurityConstants;
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
public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private Long id;

    /** 租户名称 */
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String name;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 地址 */
    @Size(max = 200, message = "地址不能超过200个字符")
    private String address;

    /** 租户状态 */
    @NotNull(message = "租户状态不能为空")
    private Integer status;

    /** 密码加密类型：MD5、BCRYPT */
    private String passwordType = SecurityConstants.PASSWORD_TYPE_MD5;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 修改人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
