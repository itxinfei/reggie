package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 关联用户ID */
    private Long userId;
    /** 会员等级ID */
    private Long levelId;
    /** 会员姓名 */
    private String name;
    /** 手机号 */
    private String phone;
    /** 积分 */
    private Long points;
    /** 余额 */
    private BigDecimal balance;
    /** 累计消费金额 */
    private BigDecimal totalConsumption;
    /** 状态（0禁用 1启用） */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
}
