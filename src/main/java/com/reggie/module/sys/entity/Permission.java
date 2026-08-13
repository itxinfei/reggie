package com.reggie.module.sys.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体
 * 定义系统中的所有操作权限（菜单权限、按钮权限、数据权限）
 */
@Data
@TableName("permission")
@Schema(description = "权限实体")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限类型：菜单 */
    public static final int TYPE_MENU = 1;
    /** 权限类型：按钮 */
    public static final int TYPE_BUTTON = 2;
    /** 权限类型：数据 */
    public static final int TYPE_DATA = 3;

    @Schema(description = "权限ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "权限名称", example = "菜品管理", required = true)
    private String permissionName;

    @Schema(description = "权限标识", example = "dish:view", required = true)
    private String permissionKey;

    @Schema(description = "权限类型：1=菜单，2=按钮，3=数据", example = "1")
    private Integer permissionType;

    @Schema(description = "父权限ID（0表示顶级）", example = "0")
    private Long parentId;

    @Schema(description = "路由路径（菜单权限用）", example = "/dish")
    private String routePath;

    @Schema(description = "菜单图标", example = "el-icon-dish")
    private String icon;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
