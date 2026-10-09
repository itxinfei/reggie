package com.reggie.module.common.controller;

import com.reggie.common.R;
import com.reggie.common.BaseContext;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.dish.service.DishEvaluationService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.store.dto.ShopSettingsDTO;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家信息接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/restaurant")
@Slf4j
// 修改点：移除类级别 @RequireEmployee，/info 接口 C 端首页/下单页需要访问
@Tag(name = "商家信息", description = "获取商家基本信息、配送参数等")
public class RestaurantController {

    @Autowired
    private StoreService storeService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DishEvaluationService dishEvaluationService;

    @Autowired
    private OrderService orderService;

    /**
     * 获取商家基本信息和配送参数。
     * <p>所有经营数据均来自真实数据源，不再硬编码：
     * 门店名取 tenant 表；评分取已通过菜品评价均值（无评价不返回）；月售取近30天已完成订单数；
     * 起送价取 StoreInfo。配送费/距离随收货地址变化，不在此给固定值，请用试算接口。</p>
     *
     * @return 商家运营信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取商家信息", description = "返回真实的商家名、评分、月售、起送价、营业时间等")
    public R<Map<String, Object>> info() {
        Long tenantId = BaseContext.getCurrentTenantId();

        // 门店扩展信息（营业时间 / 起送价 / 是否支持外卖）
        StoreInfo storeInfo = null;
        try {
            if (tenantId != null && storeService != null) {
                storeInfo = storeService.findByTenantId(tenantId);
            }
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("读取门店信息失败", e);
        }

        // 营业时间：缺省给一个默认值
        String businessHours = "09:00-22:00";
        if (storeInfo != null && storeInfo.getBusinessHours() != null && !storeInfo.getBusinessHours()
                .isEmpty()) {
            businessHours = storeInfo.getBusinessHours();
        }

        Map<String, Object> info = new HashMap<>();

        // 门店名/Logo：tenant 表是唯一名称来源（logo 列已存在：tenant.logo，此前注释"均无 logo 列"已过时）
        String name = "瑞吉外卖";
        String logo = "images/common/logo.png";
        try {
            Tenant tenant = tenantService.getById(tenantId);
            if (tenant != null) {
                if (tenant.getName() != null && !tenant.getName().isEmpty()) {
                    name = tenant.getName();
                }
                if (tenant.getLogo() != null && !tenant.getLogo().isEmpty()) {
                    // 规范化为上下文绝对路径：不同页面层级（/ 与 /front/page/）均可引用
                    String custom = tenant.getLogo();
                    if (!custom.startsWith("/") && !custom.startsWith("http://")
                            && !custom.startsWith("https://")) {
                        custom = "/" + custom;
                    }
                    logo = custom;
                }
            }
        } catch (Exception e) {
            log.warn("读取门店名称失败，使用默认名称", e);
        }
        info.put("name", name);
        info.put("logo", logo);

        // 真实评分：全部已通过菜品评价均值，四舍五入到 1 位小数；无评价则不返回（前端显示"暂无评分"）
        try {
            Double avgRating = dishEvaluationService.getStoreAverageRating(tenantId);
            if (avgRating != null) {
                double stars = Math.round(avgRating * 10.0) / 10.0;
                info.put("stars", stars);
            }
        } catch (Exception e) {
            log.warn("读取门店评分失败", e);
        }

        // 真实月售：近30天已完成订单数；为 0 则不返回
        try {
            long monthlySales = orderService.countCompletedOrdersSince(LocalDateTime.now().minusDays(30));
            if (monthlySales > 0) {
                info.put("monthlySales", monthlySales);
            }
        } catch (Exception e) {
            log.warn("读取门店月售失败", e);
        }

        // 真实起送价 + 是否支持外卖
        if (storeInfo != null) {
            if (storeInfo.getMinDeliveryAmount() != null) {
                info.put("minOrder", storeInfo.getMinDeliveryAmount());
            }
            info.put("deliveryEnabled", storeInfo.getIsDeliveryEnabled());
        }

        info.put("businessHours", businessHours);
        // 公告：store_info.notice（商家"店铺设置"可维护），无值回落默认文案
        String notice = "欢迎光临！本店精选新鲜食材，用心烹饪每一道菜品";
        if (storeInfo != null && storeInfo.getNotice() != null && !storeInfo.getNotice().isEmpty()) {
            notice = storeInfo.getNotice();
        }
        info.put("notice", notice);

        // 优惠券、配送费、距离、配送时长均不在此写死：
        // - 历史曾硬编码假券（券系统不存在、无法核销）已删除；
        // - 配送费/距离随收货地址按真实阶梯规则计算，统一走 /restaurant/delivery-fee-preview；
        // - 配送时长无真实建模，不再用"约30分钟"作履约承诺。
        return R.success(info);
    }

    /**
     * 更新店铺设置（商家自助，R-21-A 配套店铺品牌化）。
     * <p>白名单四字段：店名/Logo/公告/营业时间；目标租户强制取当前登录租户上下文，
     * 不接受前端传入 tenantId；status/phone/passwordType 等敏感字段不在此接口，
     * 仍走超管 PUT /tenant。改完 C 端首页（店名/Logo/公告/营业时间）即时生效。</p>
     *
     * @param dto 店铺设置更新对象
     * @return 更新结果
     */
    @PutMapping("/settings")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "更新店铺设置", description = "商家自助更新当前店铺名称/Logo/公告/营业时间，C端首页即时生效")
    public R<String> updateSettings(@RequestBody @Valid ShopSettingsDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("缺少租户上下文，请重新登录");
        }
        storeService.updateShopSettings(tenantId, dto.getName(), dto.getLogo(), dto.getNotice(),
                dto.getBusinessHours());
        return R.success("店铺设置已更新");
    }

    /**
     * 配送费试算：按收货地址 + 当前用户购物车实时核价，与下单实际扣费使用同一计算核心。
     *
     * @param addressBookId 收货地址ID
     * @return fee/checkEnabled/goodsAmount/distance/inRange/belowMinOrder/message 等试算字段
     */
    @RateLimit(maxRequestsPerSecond = 5)
    @GetMapping("/delivery-fee-preview")
    @Operation(summary = "配送费试算", description = "按收货地址和当前购物车实时核价试算配送费，结果与下单扣费同源")
    public R<Map<String, Object>> deliveryFeePreview(
            @RequestParam("addressBookId") Long addressBookId) {
        return R.success(orderService.previewDeliveryFee(addressBookId));
    }
}
