package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.Pattern;

/**
 * 会员
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("member")
@Schema(description = "会员")
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "关联用户ID", example = "1")
    private Long userId;

    @Schema(description = "会员等级ID", example = "1")
    private Long levelId;

    @Schema(description = "会员等级名称（逻辑字段，由 levelId 关联 member_level.name 填充，不落库）", example = "黄金会员")
    @TableField(exist = false)
    private String levelName;

    @Schema(description = "会员姓名", example = "张三")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "当前积分", example = "500")
    private Long points;

    @Schema(description = "余额（元）", example = "100.00")
    private BigDecimal balance;

    @Schema(description = "累计消费金额（元）", example = "2000.00")
    private BigDecimal totalConsumption;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
