package com.reggie.module.subsidy.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.subsidy.model.MealSubsidyAccount;
import com.reggie.module.subsidy.service.SubsidyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 餐补顾客端 Controller（C 端）
 * <p>供顾客端收银台/个人中心查询本人餐补余额与可用状态；
 * 未开通餐补（无账户）时返回 balance=0 + enabled=false，前端据此隐藏餐补支付入口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/subsidy")
@Tag(name = "餐补-顾客端", description = "顾客本人餐补余额查询")
public class SubsidyCustomerController {

    @Autowired
    private SubsidyService subsidyService;

    /**
     * 查询本人餐补余额。
     */
    @GetMapping("/my")
    @Operation(summary = "查询本人餐补余额", description = "未开通时 enabled=false、balance=0")
    public R<Map<String, Object>> my() {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (userId == null || tenantId == null) {
            return R.error("请先登录");
        }
        MealSubsidyAccount account = subsidyService.getMyAccount(userId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", account != null && Integer.valueOf(1).equals(account.getStatus()));
        result.put("balance", account != null && account.getBalance() != null
                ? account.getBalance() : BigDecimal.ZERO);
        return R.success(result);
    }
}
