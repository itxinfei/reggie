package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
import com.reggie.module.dining.model.GroupMealBooking;
import com.reggie.module.dining.model.GroupMealBookingItem;
import com.reggie.module.dining.service.GroupMealBookingService;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.setmeal.mapper.SetmealMapper;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.test.TestIdGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 团餐预订 Controller 测试（企业内部订餐）
 * <p>覆盖：创建价格快照、非法条目/日期校验、状态机（确认/完成/取消）、越权拒绝。
 * 数据挂租户 999，@Sql 建表 + TestDatabaseCleaner 按租户清理。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-dining.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class GroupMealBookingControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupMealBookingService groupMealBookingService;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private TestIdGuard testIdGuard;

    private static final String ADMIN_BYPASS_ATTRIBUTE = "SUPER_ADMIN";
    private static final long DISH_ID = 990601L;
    private static final long SETMEAL_ID = 990611L;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("group_meal_booking", "group_meal_booking_item", "dish", "setmeal", "department");
        testIdGuard.assertAbsent("dish", DISH_ID);
        testIdGuard.assertAbsent("setmeal", SETMEAL_ID);
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
        insertDish(DISH_ID, "宫保鸡丁", "20.00");
        insertSetmeal(SETMEAL_ID, "商务套餐A", "30.00");
    }

    @Test
    @DisplayName("1. 创建团餐 - 价格/名称快照与总金额正确")
    void testCreate_snapshot() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/dining/group-booking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"contactName\":\"张三\",\"contactPhone\":\"13900000000\","
                                + "\"mealDate\":\"" + LocalDate.now().plusDays(1) + "\",\"mealType\":\"LUNCH\","
                                + "\"items\":[{\"itemType\":\"DISH\",\"itemId\":" + DISH_ID + ",\"quantity\":10},"
                                + "{\"itemType\":\"SETMEAL\",\"itemId\":" + SETMEAL_ID + ",\"quantity\":5}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        GroupMealBooking booking = groupMealBookingService.getOne(
                new LambdaQueryWrapper<GroupMealBooking>().eq(GroupMealBooking::getContactName, "张三"));
        assertNotNull(booking);
        // 快照合计：20×10 + 30×5 = 350
        assertEquals(0, new BigDecimal("350").compareTo(booking.getTotalAmount()));
        assertEquals(GroupMealBooking.STATUS_PENDING, booking.getStatus());
        List<GroupMealBookingItem> items = groupMealBookingService.itemsOf(booking.getId(), 999L);
        assertEquals(2, items.size());
        assertEquals("宫保鸡丁", items.get(0).getItemName());
        assertEquals(0, new BigDecimal("20.00").compareTo(items.get(0).getPrice()));
    }

    @Test
    @DisplayName("2. 创建团餐 - 过去日期被拒")
    void testCreate_pastDateRejected() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/dining/group-booking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"contactName\":\"张三\",\"contactPhone\":\"13900000000\","
                                + "\"mealDate\":\"" + LocalDate.now().minusDays(1) + "\","
                                + "\"items\":[{\"itemType\":\"DISH\",\"itemId\":" + DISH_ID + ",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("不能早于今天")));
    }

    @Test
    @DisplayName("3. 创建团餐 - 空菜单被参数校验拦截")
    void testCreate_emptyItems() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/dining/group-booking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"contactName\":\"张三\",\"contactPhone\":\"13900000000\","
                                + "\"mealDate\":\"" + LocalDate.now().plusDays(1) + "\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. 创建团餐 - 不存在的菜品被拒（含跨租户语义）")
    void testCreate_nonexistentDish() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/dining/group-booking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"contactName\":\"张三\",\"contactPhone\":\"13900000000\","
                                + "\"mealDate\":\"" + LocalDate.now().plusDays(1) + "\","
                                + "\"items\":[{\"itemType\":\"DISH\",\"itemId\":998802,\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("菜品不存在")));
    }

    @Test
    @DisplayName("5. 状态机 - 待确认→已确认→已完成 顺向流转")
    void testStatusFlow_forward() throws Exception {
        Long id = createBooking();
        mockMvc.perform(put("/dining/group-booking/" + id + "/confirm")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        assertEquals(GroupMealBooking.STATUS_CONFIRMED, bookingStatus(id));

        mockMvc.perform(put("/dining/group-booking/" + id + "/complete")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        assertEquals(GroupMealBooking.STATUS_COMPLETED, bookingStatus(id));
    }

    @Test
    @DisplayName("6. 状态机 - 已完成的预订不能再取消")
    void testStatusFlow_cancelAfterCompleteRejected() throws Exception {
        Long id = createBooking();
        groupMealBookingService.changeStatus(id, GroupMealBooking.STATUS_CONFIRMED, 999L, 1L);
        groupMealBookingService.changeStatus(id, GroupMealBooking.STATUS_COMPLETED, 999L, 1L);

        mockMvc.perform(put("/dining/group-booking/" + id + "/cancel")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("不允许")));
        assertEquals(GroupMealBooking.STATUS_COMPLETED, bookingStatus(id));
    }

    @Test
    @DisplayName("7. 状态机 - 待确认可直接取消")
    void testStatusFlow_cancelFromPending() throws Exception {
        Long id = createBooking();
        mockMvc.perform(put("/dining/group-booking/" + id + "/cancel")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        assertEquals(GroupMealBooking.STATUS_CANCELLED, bookingStatus(id));
    }

    @Test
    @DisplayName("8. 非超管的变更操作被拒")
    void testAccess_nonAdminMutationDenied() throws Exception {
        Long id = createBooking();
        mockMvc.perform(put("/dining/group-booking/" + id + "/confirm")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(GroupMealBooking.STATUS_PENDING, bookingStatus(id));
    }

    // ==================== 辅助 ====================

    private Long createBooking() {
        GroupMealBooking booking = new GroupMealBooking();
        booking.setTenantId(999L);
        booking.setContactName("测试联系人");
        booking.setContactPhone("13900000000");
        booking.setMealDate(LocalDate.now().plusDays(1));
        booking.setMealType(GroupMealBooking.MEAL_LUNCH);
        booking.setStatus(GroupMealBooking.STATUS_PENDING);
        booking.setTotalAmount(new BigDecimal("200.00"));
        booking.setCreateUser(1L);
        booking.setUpdateUser(1L);
        groupMealBookingService.save(booking);
        return booking.getId();
    }

    private Integer bookingStatus(Long id) {
        return groupMealBookingService.getById(id).getStatus();
    }

    private void insertDish(long id, String name, String price) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setName(name);
        dish.setCategoryId(99901L);
        dish.setPrice(new BigDecimal(price));
        dish.setStatus(1);
        dish.setTenantId(999L);
        dish.setIsDeleted(0);
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.insert(dish);
    }

    private void insertSetmeal(long id, String name, String price) {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setName(name);
        setmeal.setCategoryId(99901L);
        setmeal.setPrice(new BigDecimal(price));
        setmeal.setStatus(1);
        setmeal.setTenantId(999L);
        setmeal.setIsDeleted(0);
        setmeal.setCreateTime(LocalDateTime.now());
        setmeal.setUpdateTime(LocalDateTime.now());
        setmealMapper.insert(setmeal);
    }
}
