package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderRememberToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 骑手记住登录令牌 Mapper（P2-2）。
 *
 * @author reggie
 * @since 2026-09-29
 */
@Mapper
public interface RiderRememberTokenMapper extends BaseMapper<RiderRememberToken> {
}
