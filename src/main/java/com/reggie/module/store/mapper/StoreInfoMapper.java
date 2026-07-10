package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.store.model.StoreInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 门店信息 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface StoreInfoMapper extends BaseMapper<StoreInfo> {

    /**
     * 根据租户ID查询门店信息
     */
    @Select("SELECT * FROM store_info WHERE tenant_id = #{tenantId}")
    StoreInfo findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询某总店下所有分店
     */
    @Select("SELECT * FROM store_info WHERE parent_tenant_id = #{parentTenantId}")
    List<StoreInfo> findByParentTenantId(@Param("parentTenantId") Long parentTenantId);

    /**
     * 根据门店编码查询
     */
    @Select("SELECT * FROM store_info WHERE store_code = #{storeCode}")
    StoreInfo findByStoreCode(@Param("storeCode") String storeCode);
}
