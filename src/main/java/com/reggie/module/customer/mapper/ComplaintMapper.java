package com.reggie.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.customer.model.Complaint;
import org.apache.ibatis.annotations.Mapper;

/**
 * Complaint Mapper
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}

