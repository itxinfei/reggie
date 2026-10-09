package com.reggie.module.dining.controller;

import com.reggie.common.R;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.vo.DiningTablePublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扫码点餐公开桌台信息控制器
 * <p>面向 C 端顾客：用任意扫码工具（含系统相机 / 第三方扫码 App）扫描桌上二维码后，
 * 在浏览器中打开点餐页，需匿名（无需登录、非员工）即可加载桌台信息。</p>
 * <p>本控制器刻意【不】标注 {@code @RequireEmployee}，且无员工会话依赖；
 * 由 {@code LoginCheckFilter} 的 {@code LOGIN_EXCLUDE_URLS} 放行匿名访问。
 * 仅返回名称 / 座位数 / 状态 / 区域等非敏感展示字段，不暴露 tenantId / 订单等内部数据。</p>
 *
 * @author reggie
 * @since 2026-09-17
 */
@Slf4j
@RestController
@RequestMapping("/api/dining/table/public")
@Tag(name = "扫码点餐公开桌台")
public class DiningTablePublicController {

    @Autowired
    private DiningTableService diningTableService;

    /**
     * 根据ID查询桌台公开信息（扫码点餐用，匿名可访问）
     *
     * @param id 桌台ID
     * @return 桌台公开展示信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "扫码点餐-桌台公开信息", description = "按桌台ID返回名称/座位数/状态/区域，供顾客扫码选餐，无需登录")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<DiningTablePublicVO> getPublicById(@PathVariable Long id) {
        DiningTablePublicVO table = diningTableService.getPublicById(id);
        if (table == null) {
            return R.error("没有查询到对应桌台");
        }
        return R.success(table);
    }

    /**
     * 根据桌台查询公开菜单（扫码点餐用，匿名可访问）
     * <p>租户由桌台反查确定，一次返回所属门店的菜品分类与在售菜品。</p>
     *
     * @param id 桌台ID
     * @return 公开菜单（分类 + 菜品）
     */
    @GetMapping("/{id}/menu")
    @Operation(summary = "扫码点餐-公开菜单", description = "按桌台返回所属门店的分类与在售菜品，供匿名顾客浏览，无需登录")
    @Parameter(name = "id", description = "桌台ID", required = true)
    public R<com.reggie.module.dining.vo.DiningMenuVO> getPublicMenu(@PathVariable Long id) {
        com.reggie.module.dining.vo.DiningMenuVO menu = diningTableService.getPublicMenu(id);
        if (menu == null) {
            return R.error("没有查询到对应桌台");
        }
        return R.success(menu);
    }
}
