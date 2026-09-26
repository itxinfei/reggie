package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端评价环边界测试：提交评价 / 删除自己评价 / 按订单查询 / 评分统计。
 *
 * <p>口径（实测）：</p>
 * <ul>
 *   <li>starRating 的 {@code @Min/@Max} 由 {@code @Valid} 先拦 → <b>400</b>；
 *       其余必填/归属/状态/重复校验在 Service → <b>422</b>；</li>
 *   <li>新评价默认 status=0 待审核；公开列表与评分统计只算 status=1；</li>
 *   <li>仅本人、未审核(status=0)的评价可删除。</li>
 * </ul>
 *
 * <p>数据隔离：测试订单用高 ID（9905xx），用例前按订单范围清 dish_evaluation（含他人评价），
 * 基础数据挂租户 999。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DishEvaluationControllerTest extends BaseControllerTest {

    private static final long USER_ID = 990001L;
    private static final long OTHER_USER_ID = 990002L;
    private static final long DISH_ID = 99001L;
    private static final long MISSING_DISH_ID = 990099L;

    /** 已完成订单（本人） */
    private static final long ORDER_DONE = 990501L;
    /** 配送中订单（本人，未完成） */
    private static final long ORDER_DELIVERING = 990502L;
    /** 已完成订单（属于其他用户） */
    private static final long ORDER_OTHER_USER = 990503L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // 按订单范围清评价（含 user990002 的他人评价，避免 cleaner 只删 990001 造成残留）
        cleaner.cleanByCondition("dish_evaluation",
                "order_id IN (" + ORDER_DONE + "," + ORDER_DELIVERING + "," + ORDER_OTHER_USER + ")");
        cleaner.cleanTables("order_detail", "orders", "user");
        // 清限流计数
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentId(USER_ID);
        BaseContext.setCurrentTenantId(999L);

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO user (id, name, phone, status, create_time, tenant_id) VALUES (?, ?, ?, ?, ?, ?)",
                USER_ID, "测试用户", "13800138000", 1, now, 999L);

        insertOrder(ORDER_DONE, USER_ID, 4, now);
        insertOrder(ORDER_DELIVERING, USER_ID, 3, now);
        insertOrder(ORDER_OTHER_USER, OTHER_USER_ID, 4, now);

        // 仅 ORDER_DONE 有明细（dish 99001）
        jdbcTemplate.update("INSERT INTO order_detail (id, name, order_id, dish_id, number, amount, tenant_id, create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                990701L, "测试菜品", ORDER_DONE, DISH_ID, 1, new BigDecimal("38.00"), 999L, now, now, 0);
    }

    private void insertOrder(long id, long userId, int status, LocalDateTime now) {
        jdbcTemplate.update("INSERT INTO orders (id, number, status, user_id, order_time, checkout_time, amount, user_name, phone, address, consignee, create_time, update_time, tenant_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, "N" + id, status, userId, now, now, new BigDecimal("38.00"), "下单用户", "13800138000",
                "测试地址", "收件人", now, now, 999L, 0);
    }

    /** 裸插一条评价（用于删除/统计/查询场景，可指定归属与审核状态）。 */
    private void insertEvalRow(long id, long orderId, long userId, long dishId, Integer status, Integer star) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO dish_evaluation (id, tenant_id, order_id, user_id, user_name, dish_id, dish_name, star_rating, content, anonymous, status, create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, 999L, orderId, userId, "评价用户", dishId, "测试菜品", star, "评价内容", 0, status, now, now, 0);
    }

    // ==================== 提交评价 ====================

    @Test
    void testAddSuccess() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID
                        + ",\"starRating\":5,\"content\":\"味道很好\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value(0))
                // 商品名从订单明细回填、用户名从 user 表回填
                .andExpect(jsonPath("$.data.dishName").value("测试菜品"))
                .andExpect(jsonPath("$.data.userName").value("测试用户"));
    }

    @Test
    void testAddMissingOrderId() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":" + DISH_ID + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("订单ID不能为空"));
    }

    @Test
    void testAddMissingProduct() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("评价商品不能为空"));
    }

    @Test
    void testAddOrderNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":999999,\"dishId\":" + DISH_ID + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    @Test
    void testAddNotOwner() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_OTHER_USER + ",\"dishId\":" + DISH_ID + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("无权评价该订单"));
    }

    @Test
    void testAddOrderNotCompleted() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DELIVERING + ",\"dishId\":" + DISH_ID + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("订单未完成，无法评价"));
    }

    @Test
    void testAddProductNotInOrder() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + MISSING_DISH_ID + ",\"starRating\":5}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("该商品不属于此订单"));
    }

    @Test
    void testAddDuplicate() throws Exception {
        String body = "{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID + ",\"starRating\":5}";
        // 第一次成功
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        // 第二次重复 → 拒绝
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("该商品已评价过，不能重复评价"));
    }

    @Test
    void testAddStarTooLow() throws Exception {
        // starRating=0 被 @Min 拦截 → 400
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID + ",\"starRating\":0}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("参数校验失败：starRating: 评分不能低于1分"));
    }

    @Test
    void testAddStarTooHigh() throws Exception {
        // starRating=6 被 @Max 拦截 → 400
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID + ",\"starRating\":6}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("参数校验失败：starRating: 评分不能高于5分"));
    }

    @Test
    void testAddStarMissing() throws Exception {
        // starRating 缺省(null) 通过 @Valid，到 Service 校验 → 422
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID + "}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("评分必须在1-5分之间"));
    }

    @Test
    void testAddContentTooLong() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append('a');
        }
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + ORDER_DONE + ",\"dishId\":" + DISH_ID
                        + ",\"starRating\":5,\"content\":\"" + sb.toString() + "\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("评价内容不能超过500个字符"));
    }

    // ==================== 删除自己的评价 ====================

    @Test
    void testDeleteSuccess() throws Exception {
        insertEvalRow(990601L, ORDER_DONE, USER_ID, DISH_ID, 0, 5);

        mockMvc.perform(withCsrfToken(mockMvc, delete("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":990601}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除成功"));

        // removeById 为 @TableLogic 逻辑删除（置 is_deleted=1），非物理删除
        Integer isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM dish_evaluation WHERE id = 990601", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, isDeleted == null ? 0 : isDeleted.intValue());
    }

    @Test
    void testDeleteMissingId() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, delete("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("评价ID不能为空"));
    }

    @Test
    void testDeleteNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, delete("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":999999}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("评价不存在"));
    }

    @Test
    void testDeleteNotOwner() throws Exception {
        // 评价属于 990002（同租户 999），当前用户 990001 删除 → 拒
        insertEvalRow(990602L, ORDER_OTHER_USER, OTHER_USER_ID, DISH_ID, 0, 5);

        mockMvc.perform(withCsrfToken(mockMvc, delete("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":990602}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("只能删除自己的评价"));
    }

    @Test
    void testDeleteAlreadyApproved() throws Exception {
        // status=1 已审核 → 不可删
        insertEvalRow(990603L, ORDER_DONE, USER_ID, DISH_ID, 1, 5);

        mockMvc.perform(withCsrfToken(mockMvc, delete("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":990603}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("已审核的评价无法删除"));
    }

    // ==================== 查询 / 统计 ====================

    @Test
    void testListByOrderId() throws Exception {
        insertEvalRow(990604L, ORDER_DONE, USER_ID, DISH_ID, 1, 5);

        mockMvc.perform(get("/api/dish-evaluation/order/" + ORDER_DONE)
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].id").value("990604"));
    }

    @Test
    void testRatingStatsApproved() throws Exception {
        insertEvalRow(990605L, ORDER_DONE, USER_ID, DISH_ID, 1, 5);

        mockMvc.perform(get("/api/dish-evaluation/dish/" + DISH_ID + "/stats")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.reviewCount").value(1));
    }

    @Test
    void testRatingStatsPendingNotCounted() throws Exception {
        // 仅 status=0 待审核 → 统计（只算 status=1）totalCount=0
        insertEvalRow(990606L, ORDER_DONE, USER_ID, DISH_ID, 0, 5);

        mockMvc.perform(get("/api/dish-evaluation/dish/" + DISH_ID + "/stats")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }
}
