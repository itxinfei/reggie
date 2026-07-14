package com.reggie.controller;

import com.reggie.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@Tag(name = "商家信息", description = "获取商家基本信息、配送参数等")
public class RestaurantController {

    /**
     * 获取商家基本信息和配送参数
     * @return 商家运营信息（评分、销量、配送费等）
     */
    @GetMapping("/info")
    @Operation(summary = "获取商家信息", description = "返回商家评分、销量、配送费等基本运营信息")
    public R<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        // 商家基本信息
        info.put("name", "瑞吉外卖");
        info.put("logo", "images/common/logo.png");
        info.put("brand", true);

        // 评分与销量
        info.put("stars", 4.8);
        info.put("monthlySales", "999+");

        // 配送参数
        info.put("deliveryTime", "约30分钟");
        info.put("deliveryFee", 6);
        info.put("distance", "1.5km");
        info.put("minOrder", 15);

        // 营业状态
        info.put("businessHours", "09:00-22:00");
        info.put("notice", "欢迎光临！本店精选新鲜食材，用心烹饪每一道菜品");

        // 优惠券信息（前端可据此动态展示优惠栏）
        Map<String, String> coupon1 = new HashMap<>();
        coupon1.put("tag", "减");
        coupon1.put("text", "满25减5");
        Map<String, String> coupon2 = new HashMap<>();
        coupon2.put("tag", "折");
        coupon2.put("text", "新客立减3元");
        Map<String, String> coupon3 = new HashMap<>();
        coupon3.put("tag", "减");
        coupon3.put("text", "满50减10");
        info.put("coupons", new Map[]{coupon1, coupon2, coupon3});

        return R.success(info);
    }
}
