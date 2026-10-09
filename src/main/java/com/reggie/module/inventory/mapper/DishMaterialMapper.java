package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.DishMaterial;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 菜品食材关联 Mapper
 *
 * @author reggie
 * @since 2026-08-22
 */
@Mapper
public interface DishMaterialMapper extends BaseMapper<DishMaterial> {

    @Select("SELECT dm.*, m.NAME AS material_name, m.UNIT AS material_unit FROM dish_material dm " +
            "LEFT JOIN material m ON dm.material_id = m.id " +
            "WHERE dm.dish_id = #{dishId} AND dm.tenant_id = #{tenantId} " +
            "ORDER BY dm.sort ASC")
    List<DishMaterial> listByDishId(@Param("dishId") Long dishId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM dish_material WHERE dish_id = #{dishId} AND tenant_id = #{tenantId}")
    int deleteByDishId(@Param("dishId") Long dishId, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM dish_material WHERE material_id = #{materialId} AND tenant_id = #{tenantId}")
    int countByMaterialId(@Param("materialId") Long materialId, @Param("tenantId") Long tenantId);
}
