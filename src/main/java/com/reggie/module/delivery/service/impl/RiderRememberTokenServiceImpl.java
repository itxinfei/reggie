package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.delivery.mapper.RiderRememberTokenMapper;
import com.reggie.module.delivery.model.RiderRememberToken;
import com.reggie.module.delivery.service.RiderRememberTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 骑手记住登录令牌服务实现（P2-2）。
 *
 * @author reggie
 * @since 2026-09-29
 */
@Service
public class RiderRememberTokenServiceImpl
        extends ServiceImpl<RiderRememberTokenMapper, RiderRememberToken>
        implements RiderRememberTokenService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderRememberToken issue(Long riderId, Long tenantId) {
        // 每个骑手仅一条：先删旧令牌再插入
        lambdaUpdate().eq(RiderRememberToken::getRiderId, riderId).remove();

        RiderRememberToken token = new RiderRememberToken();
        token.setRiderId(riderId);
        token.setTenantId(tenantId);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpireTime(LocalDateTime.now().plusDays(TTL_DAYS));
        save(token);
        return token;
    }

    @Override
    public RiderRememberToken validate(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        RiderRememberToken record = lambdaQuery()
                .eq(RiderRememberToken::getToken, token)
                .one();
        if (record == null || record.getExpireTime() == null
                || record.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return record;
    }

    @Override
    public void revokeByRider(Long riderId) {
        lambdaUpdate().eq(RiderRememberToken::getRiderId, riderId).remove();
    }
}
