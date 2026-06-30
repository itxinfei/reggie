package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reggie.common.SecurityConstants;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String phone;
    private String address;
    private Integer status;

    /**
     * 密码加密类型：MD5、BCRYPT
     */
    private String passwordType = SecurityConstants.PASSWORD_TYPE_MD5; // 默认MD5，兼容老数据

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
