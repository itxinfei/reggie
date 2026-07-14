package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.store.model.StoreInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 门店信息 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StoreInfoMapper extends BaseMapper<StoreInfo> {

    /**
     * 根据租户ID查询门店信息
     *
     * @param tenantId 租户ID
     * @return 门店信息
     */
    @Select("SELECT * FROM store_info WHERE tenant_id = #{tenantId}")
    StoreInfo findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询某总店下所有分店
     *
     * @param parentTenantId 总店租户ID
     * @return 分店列表
     */
    @Select("SELECT * FROM store_info WHERE parent_tenant_id = #{parentTenantId}")
    List<StoreInfo> findByParentTenantId(@Param("parentTenantId") Long parentTenantId);

    /**
     * 根据门店编码查询
     *
     * @param storeCode 门店编码
     * @return 门店信息
     */
    @Select("SELECT * FROM store_info WHERE store_code = #{storeCode}")
    StoreInfo findByStoreCode(@Param("storeCode") String storeCode);

    /**
     * 分页搜索门店列表（支持多条件筛选与排序）
     * SQL定义在 resources/com/reggie/module/store/mapper/StoreInfoMapper.xml
     *
     * @param page 分页对象
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<Map<String, Object>> searchStores(Page<?> page, @Param("keyword") String keyword,
                                            @Param("storeType") Integer storeType,
                                            @Param("status") Integer status,
                                            @Param("sortBy") String sortBy,
                                            @Param("sortOrder") String sortOrder,
                                            @Param("tenantId") Long tenantId);

    /**
     * 统计门店数量（配合筛选条件）
     *
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param tenantId 租户ID
     * @return 总数
     */
    long countStores(@Param("keyword") String keyword,
                     @Param("storeType") Integer storeType,
                     @Param("status") Integer status,
                     @Param("tenantId") Long tenantId);

    /**
     * 查询门店明细（含Tenant名称、今日经营数据）
     *
     * @param tenantId 门店ID
     * @return 门店详情Map
     */
    Map<String, Object> searchStoreDetail(@Param("tenantId") Long tenantId);

    /**
     * 导出全部门店数据（不限制数量，受筛选条件影响）
     *
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param tenantId 租户ID
     * @return 门店列表
     */
    List<Map<String, Object>> exportStores(@Param("keyword") String keyword,
                                           @Param("storeType") Integer storeType,
                                           @Param("status") Integer status,
                                           @Param("tenantId") Long tenantId);
}
