package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
