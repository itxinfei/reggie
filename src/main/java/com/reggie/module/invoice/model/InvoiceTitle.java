package com.reggie.module.invoice.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发票抬头实体
 */
@Data
@TableName("invoice_title")
@Schema(description = "发票抬头")
public class InvoiceTitle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "发票抬头")
    private String title;

    @Schema(description = "税号")
    private String taxNumber;

    @Schema(description = "公司名称（企业用）")
    private String companyName;

    @Schema(description = "类型：1=个人，2=企业")
    private Integer type;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "归属用户ID（用户端按此隔离，空为历史/公共数据不对外返回）")
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}