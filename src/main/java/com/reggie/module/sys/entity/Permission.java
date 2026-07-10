package com.reggie.module.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体
 * 定义系统中的所有操作权限（菜单权限、按钮权限、数据权限）
 */
@Data
@TableName("permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限类型：菜单 */
    public static final int TYPE_MENU = 1;
    /** 权限类型：按钮 */
    public static final int TYPE_BUTTON = 2;
    /** 权限类型：数据 */
    public static final int TYPE_DATA = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 权限名称 */
    private String permissionName;

    /** 权限标识（如dish:view/dish:edit） */
    private String permissionKey;

    /** 权限类型 1:菜单 2:按钮 3:数据 */
    private Integer permissionType;

    /** 父权限ID，0表示顶级 */
    private Long parentId;

    /** 路由路径（菜单权限用） */
    private String routePath;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 状态 0:禁用 1:启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
