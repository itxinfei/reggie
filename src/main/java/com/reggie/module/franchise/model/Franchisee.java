package com.reggie.module.franchise.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加盟商
 *
 * @author reggie
 * @since 2026-08-15
 */
@Data
@TableName("franchisee")
@Schema(description = "加盟商")
public class Franchisee implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态-启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态-禁用 */
    public static final int STATUS_DISABLED = 0;

    @Schema(description = "加盟商ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属总部租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "加盟商名称", example = "张三加盟店")
    @NotBlank(message = "加盟商名称不能为空")
    private String name;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "联系地址", example = "上海市浦东新区xxx")
    private String address;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
