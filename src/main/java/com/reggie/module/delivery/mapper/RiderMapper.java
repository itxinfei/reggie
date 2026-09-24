package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.Rider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Rider Mapper
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface RiderMapper extends BaseMapper<Rider> {

    /**
     * 骑手登录专用：按手机号查询（跨租户）。
     * <p>
     * 登录前会话无租户上下文，而 rider 表受多租户拦截器管控（上下文为空时 fail-closed
     * 会追加 tenant_id=-1 导致查空），故用 {@link InterceptorIgnore} 关闭本条查询的租户过滤。
     * 骑手端主要在单店使用；同一手机号理论上对应唯一骑手。
     * </p>
     *
     * @param phone 手机号
     * @return 骑手（含密码，仅用于服务端登录校验，不会序列化返回）
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM rider WHERE phone = #{phone} LIMIT 1")
    Rider selectByPhone(@Param("phone") String phone);
}
