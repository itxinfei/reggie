package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分记录实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PointsRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    /** 租户ID */
    private Long tenantId;

    /** 会员ID */
    private Long memberId;
    /** 类型（earn获取 consume消费） */
    private String type;
    /** 积分数量 */
    private Integer points;
    /** 关联业务类型 */
    private String bizType;
    /** 关联业务ID */
    private Long bizId;
    /** 备注说明 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdTime;
}
