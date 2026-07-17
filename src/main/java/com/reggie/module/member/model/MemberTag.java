package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reggie.enums.MemberBizTag;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签实体类
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("member_tag")
public class MemberTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 会员ID */
    private Long memberId;

    /** 标签名称 */
    private String tagName;

    /** 标签类型（1手动添加 2自动生成） */
    private Integer tagType;

    /** 业务标签 */
    private String bizTag;

    /** 标签颜色 */
    private String tagColor;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 创建用户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
