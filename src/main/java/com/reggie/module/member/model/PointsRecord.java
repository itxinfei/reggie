package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long memberId;
    private String type;
    private Integer points;
    private String bizType;
    private Long bizId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
