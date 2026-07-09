package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    // 新增/修改时需要验证姓名，登录时不验证
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50位")
    private String name;

    private String password;

    /**
     * 密码加密类型：BCRYPT
     * 注意：新注册员工默认使用 BCrypt
     * 老数据（MD5）在登录时会自动检测并升级为 BCrypt（见 EmployeeController.login() 第91-101行）
     */
    private String passwordType = SecurityConstants.PASSWORD_TYPE_BCRYPT;

    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    @Size(max = 11, message = "手机号不能超过11个字符")
    private String phone;

    // 修改点：设置默认值，避免org.springframework.dao.DataIntegrityViolationException: Field 'sex' doesn't have a default value
    private String sex = "1"; // 默认男

    // 修改点：设置默认值，避免NOT NULL约束异常
    private String idNumber = ""; // 身份证号码

    private Integer status;

    /**
     * 角色：1=超级管理员，2=普通员工
     */
    private Integer role = 2; // 默认普通员工

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
