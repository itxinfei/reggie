package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 骑手记住登录令牌（P2-2）。
 *
 * @author reggie
 * @since 2026-09-29
 */
@Data
@TableName("rider_remember_token")
@Schema(description = "骑手记住登录令牌")
public class RiderRememberToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "骑手ID")
    private Long riderId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "随机令牌")
    private String token;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
