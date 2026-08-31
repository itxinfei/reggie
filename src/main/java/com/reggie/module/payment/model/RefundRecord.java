package com.reggie.module.payment.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {
    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 业务订单ID（用户端售后申请直接关联订单） */
    private Long orderId;
    /** 支付订单ID */
    private Long paymentOrderId;
    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
    /** 退款流水号 */
    private String refundNo;
    /** 退款金额 */
    private BigDecimal amount;
    /** 退款原因 */
    private String reason;
    /** 退款状态 */
    private String status;
    /** 售后类型：1=整单退款 2=部分退款（用户端申请默认整单） */
    private Integer refundType;
    /** 申请人ID（用户端售后申请记录发起人） */
    private Long applyUserId;
    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 是否删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
