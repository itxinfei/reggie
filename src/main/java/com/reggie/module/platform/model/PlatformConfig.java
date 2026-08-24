package com.reggie.module.platform.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 外卖平台接入配置
 * <p>
 * 每条记录代表一个已接入的外卖平台（美团 / 饿了么 / 抖音 / 自营 / 其他）。
 * 凭据（appKey / appSecret / accessToken）加密存储，列表与详情接口返回时脱敏。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@TableName("platform_config")
public class PlatformConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 平台类型：MEITUAN / ELEME / DOUYIN / SELF / OTHER */
    private String platformType;

    /** 平台展示名称（如「美团外卖-总店」） */
    private String platformName;

    /** 平台侧门店 ID */
    private String shopId;

    /** 应用标识（加密存储） */
    private String appKey;

    /** 应用密钥（加密存储） */
    private String appSecret;

    /** 访问令牌（加密存储，部分平台用） */
    private String accessToken;

    /** 是否启用：0 停用 / 1 启用 */
    private Integer enabled;

    /**
     * 同步范围位标记：
     * 1=订单，2=商品，4=库存，8=营业状态；可叠加（如 1|2|8=11）
     */
    private Integer syncScope;

    /** 备注 */
    private String remark;

    /** 租户 ID（行级隔离） */
    private Long tenantId;

    /** 逻辑删除：0 未删 / 1 已删 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
