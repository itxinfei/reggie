package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class MemberLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 等级名称 */
    private String name;
    /** 最低积分要求 */
    private Long minPoints;
    /** 折扣率 */
    private BigDecimal discount;
    /** 排序字段，用于前端自定义等级显示顺序 */
    private Integer sort;
    /** 创建时间 */
    private LocalDateTime createdTime;
}
