package com.reggie.module.marketing.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * C 端满减控制器。
 * <p>面向下单顾客，仅需登录态：查询生效满减档位 + 权威凑单试算。
 * 试算结果与下单计费同源，前端进度条/凑单提示不得本地另算，避免展示与实扣不一致。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/api/marketing/full-reduction")
@Tag(name = "C端满减", description = "生效满减档位查询与凑单试算")
public class CustomerFullReductionController {

    @Autowired
    private MarketingService marketingService;

    /**
     * 当前生效满减档位
     *
     * @return 全部档位（门槛/优惠方式/优惠值）
     */
    @GetMapping("/tiers")
    @Operation(summary = "生效满减档位", description = "查询当前租户进行中的满减活动全部档位")
    public R<List<Map<String, Object>>> tiers() {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(marketingService.getActiveFullReductionTiers(tenantId));
    }

    /**
     * 满减试算（进度条/凑单）
     *
     * @param amount 商品金额（不含配送费）
     * @return 试算结果：优惠/命中档/下一档门槛/差额/进度
     */
    @GetMapping("/evaluate")
    @Operation(summary = "满减试算", description = "按商品金额试算满减优惠、命中档位与凑单差额，与下单计费同源")
    public R<Map<String, Object>> evaluate(@RequestParam("amount") BigDecimal amount) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[满减] C端试算: amount={}, userId={}", amount, userId);
        return R.success(marketingService.evaluateFullReduction(amount, userId, tenantId));
    }
}
