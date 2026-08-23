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

/**
 * 会员等级
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("member_level")
@Schema(description = "会员等级")
public class MemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "等级ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "等级名称", example = "黄金会员")
    private String name;

    @Schema(description = "升级所需最低积分", example = "1000")
    private Long minPoints;

    @Schema(description = "升级所需最高积分", example = "5000")
    private Long maxPoints;

    @Schema(description = "折扣率（如8.5折=0.85）", example = "0.85")
    private BigDecimal discount;

    @Schema(description = "等级描述", example = "享全场9.5折优惠")
    private String description;

    @Schema(description = "排序号（升序排列等级层级）", example = "2")
    private Integer sort;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
