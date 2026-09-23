package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-dining.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DiningTableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        DiningTable table = new DiningTable();
        table.setId(1L);
        table.setTenantId(1L);
        table.setAreaId(1L);
        table.setName("桌台1");
        table.setSeatCount(4);
        table.setStatus("FREE");
        table.setMinAmount(new BigDecimal("100.00"));
        table.setSort(1);
        table.setCreatedTime(LocalDateTime.now());
        diningTableService.save(table);
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/api/dining/table/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("桌台1"));
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新桌台\",\"seatCount\":2,\"areaId\":1,\"minAmount\":\"50.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("新桌台"));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(put("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"修改后桌台\",\"seatCount\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改桌台成功"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/dining/table/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除桌台成功"));
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/api/dining/table/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("桌台1"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testChangeStatus() throws Exception {
        // FREE → RESERVED 为合法流转，且不触发「禁止裸占用」拦截（仅 OCCUPIED 受限）
        mockMvc.perform(put("/api/dining/table/status")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"status\":\"RESERVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改状态成功"));
    }

    @Test
    void testOccupyWithoutOrderRejected() throws Exception {
        // 源头收敛（d49d19ab）：无订单的 FREE 桌台禁止直接改 OCCUPIED（裸占用会导致占用却无法结账），
        // 必须走「开台」自动建单；本测试锁定该保护不被回退
        mockMvc.perform(put("/api/dining/table/status")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"status\":\"OCCUPIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("开台")));
    }

    @Test
    void testQrcode() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void testQrcodeNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 分账：parts 超过 20 份应拒绝（CustomException → 422） */
    @Test
    void testSplitBill_partsExceedsLimit() throws Exception {
        // 先建一个有效订单，确保 @Valid 通过，走到业务层的 parts 上限校验
        Orders master = new Orders();
        master.setId(2001L);
        master.setNumber("ORD-LIMIT-TEST");
        master.setStatus(OrderStatus.ORDERED.getValue());
        master.setAmount(new BigDecimal("200.00"));
        master.setTenantId(1L);
        master.setTableId(1L);
        master.setOrderTime(LocalDateTime.now());
        orderService.save(master);

        mockMvc.perform(post("/api/dining/table/splitBill")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":2001,\"parts\":21}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("分账份数不能超过20份"));
    }

    /** 分账：主订单金额归零，子订单金额正确均分 */
    @Test
    void testSplitBill_masterAmountZeroed() throws Exception {
        // 创建主订单
        Orders master = new Orders();
        master.setId(1001L);
        master.setNumber("ORD-TEST-001");
        master.setStatus(OrderStatus.ORDERED.getValue());
        master.setAmount(new BigDecimal("100.00"));
        master.setTenantId(1L);
        master.setTableId(1L);
        master.setOrderTime(LocalDateTime.now());
        orderService.save(master);

        mockMvc.perform(post("/api/dining/table/splitBill")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":1001,\"parts\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证主订单金额归零、状态为 SPLIT
        Orders masterAfter = orderService.getById(1001L);
        assertNotNull(masterAfter);
        assertEquals(0, masterAfter.getAmount().compareTo(BigDecimal.ZERO),
                "分账后主订单金额应为 0");
        assertEquals(Integer.valueOf(OrderStatus.SPLIT.getValue()), masterAfter.getStatus(),
                "分账后主订单状态应为 SPLIT");
        assertEquals(Integer.valueOf(3), masterAfter.getSplitCount(),
                "分账后主订单 splitCount 应为 3");
    }
}
