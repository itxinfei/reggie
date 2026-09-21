package com.reggie.module.common.controller;

import com.reggie.common.R;
import com.reggie.common.BaseContext;
import com.reggie.common.RateLimit;
import com.reggie.module.dish.service.DishEvaluationService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

        // 门店名：tenant 表是唯一名称来源
        String name = "瑞吉外卖";
        try {
            Tenant tenant = tenantService.getById(tenantId);
            if (tenant != null && tenant.getName() != null && !tenant.getName().isEmpty()) {
                name = tenant.getName();
            }
        } catch (Exception e) {
            log.warn("读取门店名称失败，使用默认名称", e);
        }
        info.put("name", name);
        // logo 暂无数据字段（tenant/store_info 均无 logo 列），沿用品牌静态资源
        info.put("logo", "images/common/logo.png");

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
        info.put("notice", "欢迎光临！本店精选新鲜食材，用心烹饪每一道菜品");

        // 优惠券、配送费、距离、配送时长均不在此写死：
        // - 历史曾硬编码假券（券系统不存在、无法核销）已删除；
        // - 配送费/距离随收货地址按真实阶梯规则计算，统一走 /restaurant/delivery-fee-preview；
        // - 配送时长无真实建模，不再用"约30分钟"作履约承诺。
        return R.success(info);
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
