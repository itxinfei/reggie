package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dining_queue")
public class QueueRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String queueNo;
    private String phone;
    private Integer seatCount;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
