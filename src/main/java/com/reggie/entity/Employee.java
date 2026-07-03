package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.reggie.common.SecurityConstants;
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
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    // 新增/修改时需要验证姓名，登录时不验证
    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名不能超过30位")
    private String name;

    private String password;

    /**
     * 密码加密类型：MD5、BCRYPT
     */
    private String passwordType = SecurityConstants.PASSWORD_TYPE_MD5; // 默认MD5，兼容老数据

    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;

    private String sex;

    private String idNumber;//身份证号码

    private Integer status;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT) //插入时填充字段
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) //插入和更新时填充字段
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT) //插入时填充字段
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE) //插入和更新时填充字段
    private Long updateUser;

}
