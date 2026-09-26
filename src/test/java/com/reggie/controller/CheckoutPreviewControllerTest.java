package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.shopping.service.ShoppingCartService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端结算预览接口测试：{@code POST /api/order/preview}（只读核价）。
 *
 * <p>数据隔离：测试用户用高 ID（990001）避免与真实种子数据主键冲突，基础数据挂租户 999；
 * shopping_cart 无 tenant_id，cleaner 按 user_id=990001 清理。
 * 金额由服务端按菜品价重核（菜品 10 元 × 购物车数量 2 = 20 元）。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CheckoutPreviewControllerTest extends BaseControllerTest {

    /** 测试用户 ID（高 ID，避开种子数据） */
    private static final long USER_ID = 990001L;
    /** 另一个用户（越权场景） */
    private static final long OTHER_USER_ID = 990002L;
    private static final long CATEGORY_ID = 99001L;
    private static final long DISH_ID = 99001L;
    private static final long ADDRESS_ID = 99001L;
    private static final long OTHER_ADDRESS_ID = 99002L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("address_book", "dish", "category", "shopping_cart", "user");
        BaseContext.setCurrentId(USER_ID);
        BaseContext.setCurrentTenantId(999L);

        jdbcTemplate.update("INSERT INTO user (id, name, phone, status, create_time, tenant_id) VALUES (?, ?, ?, ?, ?, ?)",
                USER_ID, "测试用户", "13800138000", 1, LocalDateTime.now(), 999L);
        jdbcTemplate.update("INSERT INTO category (id, name, type, sort, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                CATEGORY_ID, "测试分类", 1, 1, LocalDateTime.now(), LocalDateTime.now(), USER_ID, USER_ID, 999L);
        jdbcTemplate.update("INSERT INTO dish (id, category_id, name, code, price, status, stock_qty, image, description, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                DISH_ID, CATEGORY_ID, "测试菜品", "T001", new BigDecimal("10.00"), 1, new BigDecimal("100"), "test.jpg", "测试",
                LocalDateTime.now(), LocalDateTime.now(), USER_ID, USER_ID, 999L);

        saveAddress(ADDRESS_ID, USER_ID);
        saveCart();
    }

    /** 保存一个地址，可指定地址 ID 与所属用户（用于越权场景）。 */
    private void saveAddress(Long id, Long userId) {
        AddressBook address = new AddressBook();
        address.setId(id);
        address.setUserId(userId);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setProvinceName("浙江省");
        address.setCityName("杭州市");
        address.setDistrictName("西湖区");
        address.setDetail("测试路1号");
        address.setIsDefault(1);
        addressBookService.save(address);
    }

    private void saveCart() {
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(USER_ID);
        cart.setDishId(DISH_ID);
        cart.setName("测试菜品");
        cart.setNumber(2);
        cart.setAmount(new BigDecimal("10.00"));
        cart.setImage("test.jpg");
        cart.setCreateTime(LocalDateTime.now());
        shoppingCartService.save(cart);
    }

    @Test
    void testPreviewSuccess() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":" + ADDRESS_ID + "}")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.details[0].name").value("测试菜品"))
                // 服务端按菜品价重核：10 × 2 = 20
                .andExpect(jsonPath("$.data.goodsAmount").value(20.00))
                .andExpect(jsonPath("$.data.originalGoodsAmount").value(20.00));
    }

    @Test
    void testPreviewMissingAddress() throws Exception {
        // 缺 addressBookId → @NotNull 校验失败
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("参数校验失败：addressBookId: 请选择收货地址"));
    }

    @Test
    void testPreviewAddressNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":999999}")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("收货地址不可用"));
    }

    @Test
    void testPreviewAddressNotOwner() throws Exception {
        // 另一个地址属于其他用户，当前用户用它试算 → 拒绝
        saveAddress(OTHER_ADDRESS_ID, OTHER_USER_ID);

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":" + OTHER_ADDRESS_ID + "}")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("收货地址不可用"));
    }

    @Test
    void testPreviewEmptyCart() throws Exception {
        jdbcTemplate.update("DELETE FROM shopping_cart WHERE user_id = ?", USER_ID);

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":" + ADDRESS_ID + "}")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("购物车为空，不能下单"));
    }
}
