package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.delivery.model.RiderRememberToken;

/**
 * 骑手记住登录令牌服务（P2-2）。
 *
 * @author reggie
 * @since 2026-09-29
 */
public interface RiderRememberTokenService extends IService<RiderRememberToken> {

    /** 记住登录 cookie 名 */
    String COOKIE_NAME = "rider_remember";

    /** 令牌有效期（天） */
    int TTL_DAYS = 30;

    /**
     * 颁发令牌：每个骑手仅保留一条，重新登录覆盖旧令牌。
     *
     * @param riderId  骑手ID
     * @param tenantId 租户ID
     * @return 新令牌（含 token 与过期时间）
     */
    RiderRememberToken issue(Long riderId, Long tenantId);

    /**
     * 校验令牌：存在且未过期才返回，否则 null。
     *
     * @param token 随机令牌
     * @return 令牌记录 / null
     */
    RiderRememberToken validate(String token);

    /**
     * 吊销某骑手的令牌（登出时调用）。
     *
     * @param riderId 骑手ID
     */
    void revokeByRider(Long riderId);
}
