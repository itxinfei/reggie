package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "用户实体")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "姓名", example = "张三")
    @Size(max = 50, message = "姓名不能超过50位")
    private String name;

    @Schema(description = "手机号", example = "13800138000", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    @Schema(description = "性别：0=女，1=男", example = "1")
    private String sex;

    @Schema(description = "身份证号", example = "110101199001011234")
    @Size(max = 18, message = "身份证号不能超过18位")
    private String idNumber;

    @Schema(description = "头像URL", example = "https://xxx.com/avatar.jpg")
    @Size(max = 500, message = "头像地址不能超过500个字符")
    private String avatar;

    @Schema(description = "状态：0=禁用，1=正常", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}
