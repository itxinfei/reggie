package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 供应商 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
