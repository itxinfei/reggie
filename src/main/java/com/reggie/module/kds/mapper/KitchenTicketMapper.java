package com.reggie.module.kds.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.kds.model.KitchenTicket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后厨工单 Mapper。工单带 tenant_id，由多租户插件自动隔离，无需跨租户 SQL。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Mapper
public interface KitchenTicketMapper extends BaseMapper<KitchenTicket> {
}
