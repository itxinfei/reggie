package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排队记录信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("dining_queue")
public class QueueRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 排队号码 */
    private String queueNo;

    /** 客户手机号 */
    private String phone;

    /** 需要座位数 */
    private Integer seatCount;

    /** 排队状态：WAITING-等待中，CALLED-已叫号，COMPLETED-已完成，CANCELLED-已取消 */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
