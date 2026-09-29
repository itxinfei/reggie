package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.shopping.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 到店自提测试（P1-2）：
 * 1. 自提交单生成取餐码、source=SELF_PICKUP、收货地址为空；
 * 2. 外卖单不传地址仍被拒（回归，确保自提豁免不影响外卖必填）。
 *
 * @author reggie
 * @since 2026-09-29
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// 下单链路会经 MaterialStockService 查 dish_material（BOM 扣料），需自建 inventory 表
@Sql(scripts = "classpath:schema-inventory.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SelfPickupOrderTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders", "dish", "dish_flavor", "category", "shopping_cart", "address_book", "user");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);

        jdbcTemplate.update("INSERT INTO user (id, name, phone, status, create_time, tenant_id) VALUES (?, ?, ?, ?, ?, ?)",
                1L, "测试用户", "13800138000", 1, java.time.LocalDateTime.now(), 999L);

        jdbcTemplate.update("INSERT INTO category (id, name, type, sort, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "测试分类", 1, 1, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 1L, 1L, 999L);
        jdbcTemplate.update("INSERT INTO dish (id, category_id, name, code, price, status, stock_qty, image, description, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, "测试菜品", "001", new BigDecimal("10.00"), 1, new BigDecimal("100"), "test.jpg", "测试", java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 1L, 1L, 999L);

        AddressBook address = new AddressBook();
        address.setId(1L);
        address.setUserId(1L);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setProvinceName("浙江省");
        address.setCityName("杭州市");
        address.setDistrictName("西湖区");
        address.setDetail("测试路1号");
        address.setIsDefault(1);
        addressBookService.save(address);

        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setDishId(1L);
        cart.setName("测试菜品");
        cart.setNumber(2);
        cart.setAmount(new BigDecimal("10.00"));
        cart.setImage("test.jpg");
        cart.setCreateTime(LocalDateTime.now());
        shoppingCartService.save(cart);
    }

    @Test
    void testSubmitSelfPickupGeneratesPickupCode() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/submit")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"SELF_PICKUP\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").exists());

        Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                "SELECT id, dining_type, pickup_code, address_book_id FROM orders WHERE tenant_id = 999 ORDER BY id DESC LIMIT 1");
        assertEquals("SELF_PICKUP", orderRow.get("dining_type"));
        assertNotNull(orderRow.get("pickup_code"), "自提订单必须生成取餐码");
        assertTrue(String.valueOf(orderRow.get("pickup_code")).length() == 6, "取餐码应为6位数字");
        assertNull(orderRow.get("address_book_id"), "自提订单不应有收货地址");
    }

    @Test
    void testTakeoutWithoutAddressRejected() throws Exception {
        // 回归：自提豁免地址校验后，外卖单不传地址仍应被拒绝，避免误伤必填约束
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/submit")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
