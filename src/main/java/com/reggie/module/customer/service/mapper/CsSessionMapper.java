package com.reggie.module.customer.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.customer.service.model.CsSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * Customer Service Session Mapper
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface CsSessionMapper extends BaseMapper<CsSession> {
}
