package com.reggie.module.favorite.service;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.favorite.mapper.UserFavoriteMapper;
import com.reggie.module.favorite.model.UserFavorite;
import com.reggie.module.favorite.service.impl.UserFavoriteServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户收藏逻辑单元测试（纯 Mockito，不连库）。
 * 覆盖：收藏/取消切换、菜品存在性校验、收藏ID查询、收藏列表实时组装与失效品标记。
 */
@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceTest {

    @Mock
    private UserFavoriteMapper userFavoriteMapper;

    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private UserFavoriteServiceImpl favoriteService;

    /**
     * 纯 Mockito 环境无 SqlSessionFactory，显式初始化实体 TableInfo 与列缓存，
     * 供 LambdaQueryWrapper 的 SFunction 列名解析（MP 懒初始化在该实体上未成功）。
     */
    @BeforeAll
    static void initTableInfo() {
        init(UserFavorite.class, "com.reggie.module.favorite.mapper.UserFavoriteMapper");
        init(Dish.class, "com.reggie.module.dish.mapper.DishMapper");
    }

    private static void init(Class<?> clazz, String namespace) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(namespace);
        TableInfoHelper.initTableInfo(assistant, clazz);
    }

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.setCurrentId(null);
        BaseContext.setCurrentTenantId(null);
    }

    private Dish dish(long id, String name, String price, int status) {
        Dish d = new Dish();
        d.setId(id);
        d.setName(name);
        d.setPrice(new BigDecimal(price));
        d.setImage("img/" + id + ".jpg");
        d.setStatus(status);
        d.setTenantId(999L);
        return d;
    }

    private UserFavorite fav(long targetId) {
        UserFavorite f = new UserFavorite();
        f.setId(targetId + 100);
        f.setUserId(1L);
        f.setTargetType(UserFavorite.TYPE_DISH);
        f.setTargetId(targetId);
        f.setTenantId(999L);
        return f;
    }

    @Test
    void toggle_notFavorited_adds() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, "宫保鸡丁", "20.00", 1));
        when(userFavoriteMapper.selectOne(any())).thenReturn(null);
        when(userFavoriteMapper.insert(any())).thenReturn(1);

        Map<String, Object> r = favoriteService.toggle(UserFavorite.TYPE_DISH, 1L);

        assertTrue((Boolean) r.get("favorited"), "未收藏时切换为已收藏");
        verify(userFavoriteMapper).insert(any());
    }

    @Test
    void toggle_alreadyFavorited_cancels() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, "宫保鸡丁", "20.00", 1));
        when(userFavoriteMapper.selectOne(any())).thenReturn(fav(1L));
        when(userFavoriteMapper.deleteById(101L)).thenReturn(1);

        Map<String, Object> r = favoriteService.toggle(UserFavorite.TYPE_DISH, 1L);

        assertFalse((Boolean) r.get("favorited"), "已收藏时切换为取消");
        verify(userFavoriteMapper).deleteById(101L);
    }

    @Test
    void toggle_dishMissing_throws() {
        when(dishMapper.selectById(9L)).thenReturn(null);

        assertThrows(CustomException.class,
                () -> favoriteService.toggle(UserFavorite.TYPE_DISH, 9L), "菜品不存在应拦截");
    }

    @Test
    void listIds_returnsTargetIds() {
        when(userFavoriteMapper.selectList(any())).thenReturn(Arrays.asList(fav(1L), fav(2L)));

        List<Long> ids = favoriteService.listFavoriteIds(UserFavorite.TYPE_DISH);

        assertEquals(Arrays.asList(1L, 2L), ids);
    }

    @Test
    void listFavorites_assemblesLiveDishData() {
        when(userFavoriteMapper.selectList(any())).thenReturn(Arrays.asList(fav(1L), fav(2L)));
        when(dishMapper.selectList(any())).thenReturn(Arrays.asList(
                dish(1L, "宫保鸡丁", "20.00", 1),
                dish(2L, "鱼香肉丝", "18.00", 0)));

        List<Map<String, Object>> list = favoriteService.listFavorites(UserFavorite.TYPE_DISH);

        assertEquals(2, list.size());
        assertTrue((Boolean) list.get(0).get("available"));
        assertTrue((Boolean) list.get(0).get("onSale"), "status=1 在售");
        assertFalse((Boolean) list.get(1).get("onSale"), "status=0 停售但仍可展示");
    }

    @Test
    void listFavorites_dishDeletedMarksUnavailable() {
        when(userFavoriteMapper.selectList(any())).thenReturn(
                Collections.singletonList(fav(3L)));
        // 菜品已被删除：关联查询为空
        when(dishMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> list = favoriteService.listFavorites(UserFavorite.TYPE_DISH);

        assertEquals(1, list.size());
        assertFalse((Boolean) list.get(0).get("available"), "已删除菜品标记失效");
    }
}
