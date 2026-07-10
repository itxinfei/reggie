package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    private LocalDateTime createTime;

    /** 创建用户ID */
    private Long createUser;
}
