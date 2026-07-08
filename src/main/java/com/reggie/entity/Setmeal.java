package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐
 */
@Data
@TableName("setmeal")
public class Setmeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    //分类id
    @NotNull(message = "套餐分类不能为空")
    private Long categoryId;


    //套餐名称
    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称不能超过64个字符")
    private String name;


    //套餐价格
    @NotNull(message = "套餐价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "套餐价格必须大于0")
    private BigDecimal price;


    //状态 0:停用 1:启用
    @NotNull(message = "套餐状态不能为空")
    private Integer status;


    //编码
    @Size(max = 64, message = "编码不能超过64个字符")
    private String code;


    //描述信息
    @Size(max = 400, message = "描述信息不能超过400个字符")
    private String description;


    //图片
    @Size(max = 200, message = "图片路径不能超过200个字符")
    private String image;


    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;


    //是否删除
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

}
