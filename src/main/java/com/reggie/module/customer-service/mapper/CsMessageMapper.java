package com.reggie.module.customer_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.customer_service.model.CsMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Customer Service Message Mapper
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface CsMessageMapper extends BaseMapper<CsMessage> {
}

