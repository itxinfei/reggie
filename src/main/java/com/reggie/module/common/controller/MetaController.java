package com.reggie.module.common.controller;

import com.reggie.common.R;
import com.reggie.enums.CouponStatus;
import com.reggie.enums.DeliveryOrderStatus;
import com.reggie.enums.DiningTableStatus;
import com.reggie.enums.DishStatus;
import com.reggie.enums.OrderSource;
import com.reggie.enums.OrderStatus;
import com.reggie.enums.PointsRecordType;
import com.reggie.enums.QueueRecordStatus;
import com.reggie.enums.ReservationStatus;
import com.reggie.enums.StockRecordType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据 / 枚举字典接口
 * <p>
 * 背景：订单状态等枚举此前在两端各自硬编码，且互不一致——
 * 后台 order/list.html 用 0 基（1=待接单），C 端 common.js 用 1 基且 2/3 语义与后端相反，
 * 前端 order.html 的 Tab 又与 common.js 相反，共 4 套映射。
 * 本接口把后端枚举作为唯一真源对外暴露，两端启动时拉取并缓存，页面不再写死数字。
 * </p>
 * <p>
 * 注意：本接口供后台与 C 端共同使用，因此<b>不能</b>加 @RequireEmployee，
 * 否则 C 端顾客拿不到字典（这正是 recommend/ai/delivery/member 等模块已有的教训）。
 * 字典本身不含任何业务隐私数据，可安全公开。
 * </p>
 *
 * @author reggie
 * @since 2026-08-30
 */
@RestController
@RequestMapping("/api/meta")
@Slf4j
@Tag(name = "元数据", description = "枚举字典，供后台与C端共用，避免两端各自硬编码状态码")
public class MetaController {

    /**
     * 获取全部枚举字典
     *
     * @return 字典集合，结构：{ 字典名: [ {code, label}, ... ] }
     */
    @GetMapping("/enums")
    @Operation(summary = "枚举字典", description = "返回订单状态、支付方式、订单来源等字典，code 为存储值，label 为展示文案")
    public R<Map<String, Object>> enums() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();

        // 订单状态（权威定义：com.reggie.enums.OrderStatus，1 基）
        data.put("orderStatus", toItems(OrderStatus.values()));

        // 支付方式：Orders.payMethod（权威定义：1=现金，2=微信支付，3=支付宝，4=银行卡，5=会员储值，6=货到付款）
        // 与 CashierServiceImpl 收银体系一致；货到付款(6)为 C 端下单"无需线上支付直接接单"渠道
        List<Item> payMethods = new ArrayList<Item>();
        payMethods.add(new Item(1, "现金"));
        payMethods.add(new Item(2, "微信支付"));
        payMethods.add(new Item(3, "支付宝"));
        payMethods.add(new Item(4, "银行卡"));
        payMethods.add(new Item(5, "会员储值"));
        payMethods.add(new Item(6, "货到付款"));
        data.put("payMethod", payMethods);

        // 就餐方式/订单来源（与前端 order/list.html 的就餐列、C端配送方式对应）
        data.put("orderSource", toItems(OrderSource.values()));

        // 优惠券状态
        data.put("couponStatus", toItems(CouponStatus.values()));

        // 积分流水类型（IN=获取，OUT=消费）
        data.put("pointsRecordType", toItems(PointsRecordType.values()));

        // 堂食相关
        data.put("reservationStatus", toItems(ReservationStatus.values()));
        data.put("queueRecordStatus", toItems(QueueRecordStatus.values()));
        data.put("diningTableStatus", toItems(DiningTableStatus.values()));

        // 菜品状态（起售/停售）
        data.put("dishStatus", toItems(DishStatus.values()));

        // 配送单状态
        data.put("deliveryOrderStatus", toItems(DeliveryOrderStatus.values()));

        // 库存流水类型（IN=入库，OUT=出库，CHECK=盘点）
        data.put("stockRecordType", toItems(StockRecordType.values()));

        return R.success(data);
    }

    /**
     * 通用枚举转换：任何具备 getValue()/getDesc() 的枚举都可转为 [{code,label}]。
     * <p>
     * 注意：项目内枚举的 value 类型并不统一——OrderStatus/DishStatus 为 int，
     * 而 OrderSource/CouponStatus/PointsRecordType 等为 String（如 "IN"/"OUT"）。
     * 因此 code 统一用 Object 承载，保持原类型，前端比较时请用 String(code) 归一化。
     * </p>
     */
    private List<Item> toItems(Enum<?>[] values) {
        List<Item> items = new ArrayList<Item>();
        for (Enum<?> e : values) {
            try {
                Object code = e.getClass().getMethod("getValue").invoke(e);
                Object desc = e.getClass().getMethod("getDesc").invoke(e);
                if (code != null && desc != null) {
                    items.add(new Item(code, String.valueOf(desc)));
                }
            } catch (Exception ex) {
                // 枚举未实现 getValue/getDesc 时跳过，不影响其他字典
                log.debug("枚举 {} 不支持 getValue/getDesc，已跳过", e.name());
            }
        }
        return items;
    }

    /**
     * 字典项
     */
    public static class Item {
        private final Object code;
        private final String label;

        public Item(Object code, String label) {
            this.code = code;
            this.label = label;
        }

        public Object getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
