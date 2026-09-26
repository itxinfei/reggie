package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.RefundRecordService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端订单售后环接口测试：用户取消 / 确认收货 / 申请售后 / 查询售后记录。
 *
 * <p>数据隔离：测试数据全部挂租户 999，{@code @BeforeEach} 用 {@link TestDatabaseCleaner}
 * 只删租户 999 数据；用例相互独立。会话属性 C 端用 "user"（区别于后台 "employee"）。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrderAfterSaleControllerTest extends BaseControllerTest {

    /** 测试订单固定 ID（@BeforeEach 已清表，可安全复用） */
    private static final long ORDER_ID = 5001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("refund_record", "payment_order", "orders");
        // 清除限流计数器：applyRefund(3/s)、confirmReceipt(5/s) 按 USER 计数，
        // 不清会在同秒内跨方法累积导致接口返回 429
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
    }

    /** 构造一个挂在租户 999 的外卖订单；userId / status / amount 可调。 */
    private Orders buildOrder(Long userId, Integer status, String amount) {
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setNumber("T" + ORDER_ID);
        order.setUserId(userId);
        order.setStatus(status);
        order.setAmount(new BigDecimal(amount));
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());
        order.setUserName("测试用户");
        order.setPhone("13800138000");
        order.setAddress("测试地址");
        order.setConsignee("收餐人");
        order.setSource("TAKEOUT");
        order.setTenantId(999L);
        return order;
    }

    /** 为订单造一条 SUCCESS 的支付单（申请售后要求存在成功支付单）。 */
    private void givenSuccessPayment(String amount) {
        PaymentOrder payment = new PaymentOrder();
        payment.setOrderId(ORDER_ID);
        payment.setTenantId(999L);
        payment.setTradeNo("TRADE" + ORDER_ID);
        payment.setChannel("ALIPAY");
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus("SUCCESS");
        payment.setPaidTime(LocalDateTime.now());
        paymentOrderMapper.insert(payment);
    }

    // ==================== 用户取消 ====================

    @Test
    void testUserCancelSuccess() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_PENDING_PAY, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("订单已取消"));

        assertEquals(Orders.STATUS_CANCELLED, orderService.getById(ORDER_ID).getStatus());
    }

    @Test
    void testUserCancelOrderNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", "999999")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    @Test
    void testUserCancelNotOwner() throws Exception {
        // 同租户、但属于另一个用户
        orderService.save(buildOrder(2L, Orders.STATUS_PENDING_PAY, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("无权操作此订单"));
    }

    @Test
    void testUserCancelWrongStatus() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("当前状态不允许取消，如需退款请联系客服"));
    }

    // ==================== 确认收货 ====================

    @Test
    void testConfirmReceiptSuccess() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_DELIVERING, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userConfirmReceipt")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("确认收货成功"));

        assertEquals(Orders.STATUS_COMPLETED, orderService.getById(ORDER_ID).getStatus());
    }

    @Test
    void testConfirmReceiptNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userConfirmReceipt")
                .param("id", "999999")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    @Test
    void testConfirmReceiptNotOwner() throws Exception {
        orderService.save(buildOrder(2L, Orders.STATUS_DELIVERING, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userConfirmReceipt")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("无权操作此订单"));
    }

    @Test
    void testConfirmReceiptWrongStatus() throws Exception {
        // 待接单（2）不支持确认收货
        orderService.save(buildOrder(1L, Orders.STATUS_ORDERED, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userConfirmReceipt")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("当前订单状态不支持确认收货"));
    }

    // ==================== 申请售后 ====================

    @Test
    void testApplyRefundSuccess() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.refundNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.amount").value(99.00));
    }

    @Test
    void testApplyRefundWithoutUserContext() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");

        // MockMvc 默认不经过 @WebFilter 注册的 LoginCheckFilter（同线程直接调用），
        // 故显式清空当前用户，验证 Service 层的纵深防御兜底：无登录身份 → 422 请先登录
        BaseContext.setCurrentId(null);

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请先登录"));
    }

    @Test
    void testApplyRefundOrderNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", "999999")
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    @Test
    void testApplyRefundNotOwner() throws Exception {
        orderService.save(buildOrder(2L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("无权对此订单申请售后"));
    }

    @Test
    void testApplyRefundWrongStatus() throws Exception {
        // 配送中（3）不可申请售后，仅已完成可申请
        orderService.save(buildOrder(1L, Orders.STATUS_DELIVERING, "99.00"));
        givenSuccessPayment("99.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("仅已完成订单可申请售后，其他状态请联系客服"));
    }

    @Test
    void testApplyRefundDuplicate() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");
        // 第一次申请成功（产生 PENDING 售后单）
        refundRecordService.applyUserRefund(ORDER_ID, "第一次申请");

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "再次申请")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("该订单已有售后申请处理中，请勿重复申请"));
    }

    @Test
    void testApplyRefundNoSuccessPayment() throws Exception {
        // 已完成但没有 SUCCESS 支付单
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "商品变质")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("未找到该订单对应的成功支付单，无法申请售后"));
    }

    @Test
    void testApplyRefundBlankReason() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/userApplyRefund")
                .param("id", String.valueOf(ORDER_ID))
                .param("reason", "  ")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("退款原因不能为空"));
    }

    // ==================== 查询售后记录 ====================

    @Test
    void testUserRefundRecordsSuccess() throws Exception {
        orderService.save(buildOrder(1L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");
        refundRecordService.applyUserRefund(ORDER_ID, "种子售后单");

        mockMvc.perform(get("/order/userRefundRecords")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].refundNo").isNotEmpty())
                .andExpect(jsonPath("$.data[0].reason").value("种子售后单"));
    }

    @Test
    void testUserRefundRecordsNotOwner() throws Exception {
        orderService.save(buildOrder(2L, Orders.STATUS_COMPLETED, "99.00"));
        givenSuccessPayment("99.00");

        mockMvc.perform(get("/order/userRefundRecords")
                .param("id", String.valueOf(ORDER_ID))
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("无权查询此订单的售后记录"));
    }
}
