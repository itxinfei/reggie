package com.reggie.module.sys.model;

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
 * 部门实体（企业组织架构地基）
 * <p>
 * 企业内部订餐场景的组织单元：员工可挂部门，后续餐补发放、按部门对账、
 * 批量团餐预订均以部门为维度。小型餐饮店可不用（部门为可选能力）。
 * </p>
 */
@Data
@TableName("department")
@Schema(description = "部门实体")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "部门名称", example = "技术部", required = true)
    private String name;

    @Schema(description = "部门编码", example = "TECH")
    private String code;

    @Schema(description = "负责人姓名", example = "张三")
    private String leaderName;

    @Schema(description = "备注", example = "研发中心")
    private String remark;

    @Schema(description = "排序（数值越小越靠前）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

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
    @TableLogic
    private Integer isDeleted;
}
