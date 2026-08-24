package com.reggie.module.platform.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.platform.adapter.impl.ElemeAdapter;
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
    }

    /** 通过反射调用 private parseOrders，避免依赖真实 HTTP */
    @SuppressWarnings("unchecked")
    private List<PlatformOrder> invokeParse(Object adapter, String body) throws Exception {
        Method m = adapter.getClass().getDeclaredMethod("parseOrders", String.class);
        m.setAccessible(true);
        JsonNode node = mapper.readTree(body);
        // parseOrders 接收的是原始 body 字符串
        return (List<PlatformOrder>) m.invoke(adapter, body);
    }
}
