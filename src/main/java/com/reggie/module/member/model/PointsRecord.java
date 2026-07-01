package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long memberId;
    private String type;
    private Integer points;
    private String bizType;
    private Long bizId;
    private String remark;
    private LocalDateTime createdTime;
}
