package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行政区划数据
 * 省市区三级级联，通过parent_id自关联
 */
@Data
@Schema(description = "行政区划实体")
public class Region implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "区划ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "地区名称", example = "北京市")
    private String name;

    @Schema(description = "行政区划代码", example = "110000")
    private String code;

    @Schema(description = "父级ID（0为省份）", example = "0")
    private Long parentId;

    @Schema(description = "层级：1=省，2=市，3=区/县，4=街道/乡镇", example = "1")
    private Integer level;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @Schema(description = "子节点列表（非数据库字段，用于树形结构）")
    @TableField(exist = false)
    private List<Region> children;
}
