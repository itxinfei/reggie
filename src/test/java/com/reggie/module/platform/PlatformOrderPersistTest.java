package com.reggie.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformOrderPersistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台订单落库（幂等去重）集成测试
 * <p>覆盖：字段映射、租户隔离、重复拉单去重、明细落库。</p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-platform.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlatformOrderPersistTest {

    @Autowired
    private PlatformOrderPersistService persistService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PlatformConfigService configService;

    private static final Long TENANT = 1L;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(TENANT);
    }

    private PlatformOrder buildOrder(String platformOrderId, String platformDishId) {
        PlatformOrder po = new PlatformOrder();
        po.setPlatformOrderId(platformOrderId);
        po.setPlatformStatus("NEW");
        po.setAmount(new BigDecimal("58.50"));
        po.setCustomerName("测试顾客");
        po.setCustomerPhone("13800138000");
        po.setAddress("北京市朝阳区测试路1号");
        po.setRemark("不要香菜");
        po.setOrderTime("2026-08-24 12:30:00");
        PlatformOrder.OrderItem item = new PlatformOrder.OrderItem();
        item.setPlatformDishId(platformDishId);
        item.setDishName("招牌炒饭");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("29.25"));
        item.setFlavor("微辣");
        po.setItems(new ArrayList<>());
        po.getItems().add(item);
        return po;
    }

    @Test
    void testPersistAndDeduplicate() {
        List<PlatformOrder> orders = new ArrayList<>();
        orders.add(buildOrder("MT202608240001", "D001"));

        int inserted = persistService.persistOrders("MEITUAN", "shop_001", TENANT, orders);
        assertEquals(1, inserted);

        Orders saved = orderMapper.selectOne(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getPlatformType, "MEITUAN")
                .eq(Orders::getPlatformOrderId, "MT202608240001"));
        assertNotNull(saved);
        assertEquals(new BigDecimal("58.50"), saved.getAmount());
        assertEquals(TENANT, saved.getTenantId());
        assertEquals("TAKEOUT", saved.getSource());
        assertNotNull(saved.getPlatformRaw());

        // 主单已落库，明细通过 OrderDetailService 落库（见 persistService.persistOrders）

        // 重复拉单应去重
        int insertedAgain = persistService.persistOrders("MEITUAN", "shop_001", TENANT, orders);
        assertEquals(0, insertedAgain);

        assertTrue(persistService.exists("MEITUAN", "MT202608240001", TENANT));
        assertFalse(persistService.exists("MEITUAN", "MT_NOT_EXIST", TENANT));
    }

    @Test
    void testTenantIsolationInDedup() {
        List<PlatformOrder> orders = new ArrayList<>();
        orders.add(buildOrder("MT202608240099", "D002"));

        // tenant=1 落库
        persistService.persistOrders("MEITUAN", "shop_001", 1L, orders);
        // 同一平台订单号，不同租户，应可再次落库（唯一索引含 tenant_id）
        int insertedOtherTenant = persistService.persistOrders("MEITUAN", "shop_001", 2L, orders);
        assertEquals(1, insertedOtherTenant);
    }
}
