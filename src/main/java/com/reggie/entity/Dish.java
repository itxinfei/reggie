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
 * 菜品
 */
@Data
@TableName("dish")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 菜品名称 */
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称不能超过64个字符")
    private String name;


    /** 菜品分类id */
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;


    /** 菜品价格 */
    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "菜品价格必须大于0")
    private BigDecimal price;


    /** 商品码 */
    @Size(max = 64, message = "商品码不能超过64个字符")
    private String code;


    /** 图片 */
    @Size(max = 200, message = "图片路径不能超过200个字符")
    private String image;


    /** 描述信息 */
    @Size(max = 400, message = "描述信息不能超过400个字符")
    private String description;


    /** 状态 0 停售 1 起售 */
    @NotNull(message = "菜品状态不能为空")
    private Integer status;


    /** 顺序 */
    private Integer sort;


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


    /** 是否删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /** 当前库存数量 */
    private java.math.BigDecimal stockQty;

    /** 最低库存预警阈值 */
    private java.math.BigDecimal minStock;

}
