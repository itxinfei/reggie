package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dish flavor
 */
@Data
public class DishFlavor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // Name
    @NotBlank(message = "Flavor name cannot be empty")
    @Size(max = 64, message = "Flavor name cannot exceed 64 characters")
    private String name;

    // Flavor value
    @NotBlank(message = "Flavor value cannot be empty")
    @Size(max = 64, message = "Flavor value cannot exceed 64 characters")
    private String value;

    // Dish ID
    @NotNull(message = "Dish ID cannot be empty")
    private Long dishId;

    // Tenant ID
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    // Create time
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // Update time
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // Create user
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    // Update user
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    // Is deleted
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

}
