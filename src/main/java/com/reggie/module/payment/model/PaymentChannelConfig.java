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
import java.time.LocalDateTime;

/**
 * 支付渠道配置
 * <p>
 * 每条记录代表一个租户接入的一个支付渠道（微信 APIv3 / 支付宝）。
 * 微信 APIv3 密钥、微信商户私钥、支付宝应用私钥三项加密存储，列表与详情返回时掩码；
 * 其余 AppID / 商户号 / 证书序列号 / 公钥 / 回调地址明文保存。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
@TableName("payment_channel_config")
public class PaymentChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置名称（如「微信支付-总店」） */
    private String configName;

    /** 渠道：WECHAT / ALIPAY */
    private String channel;

    /** 微信 AppID（公众号/小程序/移动应用） */
    private String wxAppId;

    /** 微信商户号 mchid */
    private String wxMchId;

    /** 微信 APIv3 密钥（加密存储） */
    private String wxApiV3Key;

    /** 微信商户证书序列号 */
    private String wxMchCertSerialNo;

    /** 微信商户 API 私钥 PEM（加密存储） */
    private String wxMchPrivateKey;

    /** 微信支付公钥 ID（公钥模式可选） */
    private String wxPublicKeyId;

    /** 微信支付公钥 PEM（公钥模式，明文） */
    private String wxPublicKey;

    /** 支付宝应用 APPID */
    private String aliAppId;

    /** 支付宝应用私钥 PEM（加密存储） */
    private String aliPrivateKey;

    /** 支付宝公钥 PEM（明文） */
    private String aliPublicKey;

    /** 支付结果异步通知地址 */
    private String payNotifyUrl;

    /** 退款异步通知地址（微信） */
    private String refundNotifyUrl;

    /** 是否启用：0 停用 / 1 启用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 租户 ID（行级隔离） */
    private Long tenantId;

    /** 逻辑删除：0 未删 / 1 已删 */
    @TableLogic
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
