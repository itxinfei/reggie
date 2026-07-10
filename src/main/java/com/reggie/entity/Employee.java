package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.reggie.common.SecurityConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工实体
 */
@Data
@Schema(description = "员工实体")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "员工ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名", example = "admin", required = true)
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    @Schema(description = "姓名", example = "张三", required = true)
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50位")
    private String name;

    @Schema(description = "密码", example = "123456", required = true)
    private String password;

    @Schema(description = "密码加密类型", example = "BCRYPT")
    private String passwordType = SecurityConstants.PASSWORD_TYPE_BCRYPT;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    @Size(max = 11, message = "手机号不能超过11个字符")
    private String phone;

    @Schema(description = "性别", example = "1")
    private String sex = "1";

    @Schema(description = "身份证号码", example = "110101199001011234")
    private String idNumber = "";

    @Schema(description = "账号状态：0=禁用，1=正常", example = "1")
    private Integer status;

    @Schema(description = "角色：1=超级管理员，2=普通员工", example = "2")
    private Integer role = 2;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

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
