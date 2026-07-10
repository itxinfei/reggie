package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 姓名 */
    @Size(max = 50, message = "姓名不能超过50位")
    private String name;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    /** 性别 0 女 1 男 */
    private String sex;

    /** 身份证号 */
    @Size(max = 18, message = "身份证号不能超过18位")
    private String idNumber;

    /** 头像 */
    @Size(max = 500, message = "头像地址不能超过500个字符")
    private String avatar;

    /** 状态 0:禁用，1:正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}
