package com.reggie.module.urgency.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.urgency.service.UrgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * C 端顾客催单控制器。
 * <p>与商家端 {@link UrgencyController} 区分：面向下单顾客，仅需登录态，不要求员工身份。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/api/urgency/customer")
@Tag(name = "C端催单", description = "顾客对自己的待接单/配送中订单发起催单")
public class CustomerUrgencyController {

    @Autowired
    private UrgencyService urgencyService;

    /**
     * 顾客催单
     *
     * @param orderId 订单ID
     * @return 催单结果（含今日已用/剩余次数）
     */
    @PostMapping("/trigger/{orderId}")
    @Operation(summary = "顾客催单", description = "对本人的待接单/配送中订单催单，每天最多3次")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    public R<Map<String, Object>> trigger(@PathVariable Long orderId) {
        Long currentUserId = BaseContext.getCurrentId();
        log.info("[催单] 顾客发起催单: orderId={}, userId={}", orderId, currentUserId);
        return urgencyService.customerTrigger(orderId, currentUserId);
    }

    /**
     * 催单频率查询
     *
     * @return 今日已催次数/剩余次数，供按钮置灰与提示
     */
    @GetMapping("/frequency")
    @Operation(summary = "催单频率查询", description = "查询今日已催次数与剩余次数")
    public R<Map<String, Object>> frequency() {
        return urgencyService.checkFrequency(BaseContext.getCurrentId());
    }
}
