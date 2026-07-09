package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行政区划数据
 * 省市区三级级联，通过parent_id自关联
 */
@Data
public class Region implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 地区名称 */
    private String name;

    /** 行政区划代码 */
    private String code;

    /** 父级ID，0为省份 */
    private Long parentId;

    /** 层级：1省 2市 3区/县 4街道/乡镇 */
    private Integer level;

    /** 排序 */
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

    /** 子节点列表（非数据库字段，用于树形结构） */
    @TableField(exist = false)
    private java.util.List<Region> children;
}
