package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CouponUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long memberId;
    private Long templateId;
    private String code;
    private String status;
    private LocalDateTime usedTime;
    private Long orderId;
    private LocalDateTime expireTime;
    private LocalDateTime createdTime;
}
