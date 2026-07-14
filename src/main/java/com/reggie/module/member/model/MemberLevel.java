package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

    @Schema(description = "折扣率（如8.5折=0.85）", example = "0.85")
    private BigDecimal discount;

    @Schema(description = "排序号（升序排列等级层级）", example = "2")
    private Integer sort;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;
}
