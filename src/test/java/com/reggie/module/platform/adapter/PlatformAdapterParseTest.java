package com.reggie.module.platform.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.platform.adapter.impl.DouyinAdapter;
import com.reggie.module.platform.adapter.impl.ElemeAdapter;
import com.reggie.module.platform.adapter.impl.JdAdapter;
import com.reggie.module.platform.adapter.impl.MeituanAdapter;
import com.reggie.module.platform.model.PlatformConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台适配器订单解析单元测试（验证不同平台响应格式的正确解析与字段映射）
 *
 * @author reggie
 * @since 2026-08-24
 */
class PlatformAdapterParseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testElemeParse() throws Exception {
        String body = "{\"result\":{\"orders\":[{\"orderId\":\"E123\",\"status\":\"NEW\","
                + "\"totalPrice\":\"58.50\",\"customerName\":\"张三\",\"phone\":\"13800138000\","
                + "\"address\":\"北京\",\"remark\":\"少辣\","
                + "\"items\":[{\"itemId\":\"D1\",\"name\":\"炒饭\",\"quantity\":2,\"price\":\"29.25\",\"spec\":\"微辣\"}]}]}}";
        ElemeAdapter adapter = new ElemeAdapter();
        List<PlatformOrder> orders = invokeParse(adapter, body);
        assertEquals(1, orders.size());
        PlatformOrder po = orders.get(0);
        assertEquals("E123", po.getPlatformOrderId());
        assertEquals(new BigDecimal("58.50"), po.getAmount());
        assertEquals("张三", po.getCustomerName());
        assertEquals(1, po.getItems().size());
        assertEquals("炒饭", po.getItems().get(0).getDishName());
        assertEquals(Integer.valueOf(2), po.getItems().get(0).getQuantity());
    }

    @Test
    void testMeituanParse() throws Exception {
        String body = "{\"code\":0,\"data\":{\"orderList\":[{\"orderId\":\"M456\",\"status\":\"PAID\","
                + "\"total\":\"99.00\",\"recipientName\":\"李四\",\"recipientPhone\":\"13900139000\","
                + "\"address\":\"上海\",\"caution\":\"勿触\","
                + "\"detail\":[{\"appFoodCode\":\"D9\",\"foodName\":\"拉面\",\"quantity\":1,\"price\":\"99.00\",\"spec\":\"大碗\"}]}]}}";
        MeituanAdapter adapter = new MeituanAdapter();
        List<PlatformOrder> orders = invokeParse(adapter, body);
        assertEquals(1, orders.size());
        PlatformOrder po = orders.get(0);
        assertEquals("M456", po.getPlatformOrderId());
        assertEquals(new BigDecimal("99.00"), po.getAmount());
        assertEquals("李四", po.getCustomerName());
        assertEquals("拉面", po.getItems().get(0).getDishName());
    }

    @Test
    void testPlatformType() {
        assertEquals("ELEME", new ElemeAdapter().platformType());
        assertEquals("MEITUAN", new MeituanAdapter().platformType());
        assertEquals("DOUYIN", new DouyinAdapter().platformType());
        assertEquals("JD", new JdAdapter().platformType());
    }

    /** 通过反射调用 private parseOrders，避免依赖真实 HTTP */
    @Test
    void testDouyinParse() throws Exception {
        // 抖音生活服务：err_no/data.order_list 包裹，snake_case 字段
        String body = "{\"err_no\":0,\"err_msg\":\"success\",\"data\":{\"order_list\":["
                + "{\"order_id\":\"DY789\",\"order_status\":\"PAID\",\"total_amount\":\"66.00\","
                + "\"contact_name\":\"王五\",\"contact_phone\":\"13700137000\","
                + "\"address_detail\":\"深圳南山\",\"remark\":\"多放醋\",\"create_time\":\"2026-09-12 12:00:00\","
                + "\"item_list\":[{\"spu_id\":\"DY-D1\",\"spu_name\":\"酸辣粉\",\"count\":2,\"unit_price\":\"33.00\",\"sku_spec\":\"中辣\"}]}]}}";
        List<PlatformOrder> orders = invokeParse(new DouyinAdapter(), body);
        assertEquals(1, orders.size());
        PlatformOrder po = orders.get(0);
        assertEquals("DY789", po.getPlatformOrderId());
        assertEquals(new BigDecimal("66.00"), po.getAmount());
        assertEquals("王五", po.getCustomerName());
        assertEquals("深圳南山", po.getAddress());
        assertEquals(1, po.getItems().size());
        assertEquals("酸辣粉", po.getItems().get(0).getDishName());
        assertEquals(Integer.valueOf(2), po.getItems().get(0).getQuantity());
        assertEquals("中辣", po.getItems().get(0).getFlavor());
    }

    @Test
    void testJdParse() throws Exception {
        // 京东外卖：code/result 数组包裹，camelCase 字段
        String body = "{\"code\":\"0\",\"msg\":\"success\",\"result\":["
                + "{\"orderId\":\"JD321\",\"status\":\"WAIT_ACCEPT\",\"totalFee\":\"88.80\","
                + "\"consigneeName\":\"赵六\",\"consigneePhone\":\"13600136000\","
                + "\"address\":\"广州天河\",\"remark\":\"尽快送达\",\"createTime\":\"2026-09-12 12:05:00\","
                + "\"skuList\":[{\"skuId\":\"JD-D1\",\"skuName\":\"黄焖鸡\",\"skuNum\":1,\"skuPrice\":\"88.80\",\"skuSpec\":\"微辣\"}]}]}";
        List<PlatformOrder> orders = invokeParse(new JdAdapter(), body);
        assertEquals(1, orders.size());
        PlatformOrder po = orders.get(0);
        assertEquals("JD321", po.getPlatformOrderId());
        assertEquals(new BigDecimal("88.80"), po.getAmount());
        assertEquals("赵六", po.getCustomerName());
        assertEquals("广州天河", po.getAddress());
        assertEquals(1, po.getItems().size());
        assertEquals("黄焖鸡", po.getItems().get(0).getDishName());
        assertEquals(Integer.valueOf(1), po.getItems().get(0).getQuantity());
        assertEquals("微辣", po.getItems().get(0).getFlavor());
    }

    @Test
    void testEmptyBodyReturnsEmptyList() throws Exception {
        // 非预期/空结构返回空列表而非抛异常，保证单渠道失败不影响主流程
        assertEquals(0, invokeParse(new DouyinAdapter(), "{\"err_no\":0,\"data\":{}}").size());
        assertEquals(0, invokeParse(new JdAdapter(), "{\"code\":\"0\"}").size());
    }

    @SuppressWarnings("unchecked")
    private List<PlatformOrder> invokeParse(Object adapter, String body) throws Exception {
        Method m = adapter.getClass().getDeclaredMethod("parseOrders", String.class);
        m.setAccessible(true);
        JsonNode node = mapper.readTree(body);
        // parseOrders 接收的是原始 body 字符串
        return (List<PlatformOrder>) m.invoke(adapter, body);
    }
}
