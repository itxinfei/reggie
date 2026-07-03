package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 地址簿
 */
@Data
public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;


    //用户id
    @NotNull(message = "用户ID不能为空")
    private Long userId;


    //收货人
    @NotBlank(message = "收货人不能为空")
    @Size(max = 30, message = "收货人姓名不能超过30个字符")
    private String consignee;


    //手机号
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;


    //性别 0 女 1 男
    @Size(max = 5, message = "性别格式不正确")
    private String sex;


    //省级区划编号
    @NotBlank(message = "省级区划编号不能为空")
    private String provinceCode;


    //省级名称
    @NotBlank(message = "省级名称不能为空")
    private String provinceName;


    //市级区划编号
    @NotBlank(message = "市级区划编号不能为空")
    private String cityCode;


    //市级名称
    @NotBlank(message = "市级名称不能为空")
    private String cityName;


    //区级区划编号
    @NotBlank(message = "区级区划编号不能为空")
    private String districtCode;


    //区级名称
    @NotBlank(message = "区级名称不能为空")
    private String districtName;


    //详细地址
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址不能超过200个字符")
    private String detail;


    //标签
    @Size(max = 20, message = "标签不能超过20个字符")
    private String label;

    //是否默认 0 否 1是
    private Integer isDefault;

    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    //更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    //创建人
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    //修改人
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;


    //是否删除
    private Integer isDeleted;
}
