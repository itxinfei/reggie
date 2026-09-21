package com.reggie.module.favorite.controller;

import com.reggie.common.R;
import com.reggie.module.favorite.service.UserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * C 端用户收藏控制器（仅需登录态，由全局过滤器统一拦截）。
 */
@Slf4j
@RestController
@RequestMapping("/api/favorite")
@Tag(name = "C端收藏", description = "菜品收藏的切换/列表/状态查询")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService favoriteService;

    /**
     * 切换收藏（已收藏取消、未收藏加入）
     */
    @PostMapping("/toggle")
    @Operation(summary = "切换收藏", description = "切换当前用户对指定对象的收藏状态")
    public R<Map<String, Object>> toggle(@RequestBody Map<String, Object> body) {
        Integer targetType = parseInteger(body.get("targetType"));
        Long targetId = parseLong(body.get("targetId"));
        log.info("[收藏] 切换: targetType={}, targetId={}", targetType, targetId);
        return R.success(favoriteService.toggle(targetType, targetId));
    }

    /**
     * 已收藏对象ID集合（星标初始化）
     */
    @GetMapping("/ids")
    @Operation(summary = "已收藏ID", description = "查询当前用户某类型全部已收藏对象ID")
    public R<List<Long>> ids(@RequestParam("targetType") Integer targetType) {
        return R.success(favoriteService.listFavoriteIds(targetType));
    }

    /**
     * 收藏列表
     */
    @GetMapping("/list")
    @Operation(summary = "收藏列表", description = "查询当前用户收藏，菜品信息实时关联")
    public R<List<Map<String, Object>>> list(
            @RequestParam(value = "targetType", required = false) Integer targetType) {
        return R.success(favoriteService.listFavorites(targetType));
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
