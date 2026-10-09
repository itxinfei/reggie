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

    @Autowired
    private com.reggie.test.TestDatabaseCleaner cleaner;

    @Autowired
    private com.reggie.module.dining.mapper.TableAreaMapper areaMapper;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(999L);
        // 先清上次运行残留的测试主键（本类历史上不清理，第二次运行会主键冲突）；
        // 分账子订单 master_order_id 指向测试主订单，必须一并清，否则触发"已分账"幂等拦截
        cleaner.cleanByCondition("orders",
                "id IN (?, ?) OR master_order_id IN (?, ?)", 996201L, 996202L, 996201L, 996202L);
        cleaner.cleanByCondition("dining_table", "id = ?", 996001L);
        cleaner.cleanByCondition("dining_area", "id = ?", 996101L);
        // 重建桌台所属区域（历史遗留行已清，需每次重建）
        com.reggie.module.dining.model.TableArea area = new com.reggie.module.dining.model.TableArea();
        area.setId(996101L);
        area.setTenantId(999L);
        area.setName("测试区域");
        area.setSort(1);
        area.setCreatedTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        area.setIsDeleted(0);
        areaMapper.insert(area);

        DiningTable table = new DiningTable();
        // 主键 / 区域用测试专用高 ID，避开 reggie 库租户 1 演示数据占用的小主键（单库改造，2026-10-02）
        table.setId(996001L);
        table.setTenantId(999L);
        table.setAreaId(996101L);
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
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("桌台1"));
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新桌台\",\"seatCount\":2,\"areaId\":996101,\"minAmount\":\"50.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("新桌台"));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(put("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":996001,\"name\":\"修改后桌台\",\"seatCount\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改桌台成功"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/dining/table/996001")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除桌台成功"));
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/api/dining/table/996001")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("桌台1"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testChangeStatus() throws Exception {
        // FREE → RESERVED 为合法流转，且不触发「禁止裸占用」拦截（仅 OCCUPIED 受限）
        mockMvc.perform(put("/api/dining/table/status")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":996001,\"status\":\"RESERVED\"}"))
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
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":996001,\"status\":\"OCCUPIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("开台")));
    }

    @Test
    void testQrcode() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/996001")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void testQrcodeNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testQrcodePoster() throws Exception {
        // 海报端点：返回 base64 PNG（schema-dining 无 tenant，storeName 走空串兜底）
        mockMvc.perform(get("/api/dining/table/qrcode/poster")
                .param("tableId", "996001")
                .param("siteUrl", "http://localhost:8080")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test
    void testQrcodePosterTableNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/poster")
                .param("tableId", "999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("桌台不存在"));
    }

    /** 分账：parts 超过 20 份应拒绝（CustomException → 422） */
    @Test
    void testSplitBill_partsExceedsLimit() throws Exception {
        // 先建一个有效订单，确保 @Valid 通过，走到业务层的 parts 上限校验
        Orders master = new Orders();
        master.setId(996202L);
        master.setNumber("ORD-LIMIT-TEST");
        master.setStatus(OrderStatus.ORDERED.getValue());
        master.setAmount(new BigDecimal("200.00"));
        master.setTenantId(999L);
        master.setTableId(996001L);
        master.setOrderTime(LocalDateTime.now());
        orderService.save(master);

        mockMvc.perform(post("/api/dining/table/splitBill")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":996202,\"parts\":21}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value("分账份数不能超过20份"));
    }

    /** 分账：主订单金额归零，子订单金额正确均分 */
    @Test
    void testSplitBill_masterAmountZeroed() throws Exception {
        // 创建主订单
        Orders master = new Orders();
        master.setId(996201L);
        master.setNumber("ORD-TEST-001");
        master.setStatus(OrderStatus.ORDERED.getValue());
        master.setAmount(new BigDecimal("100.00"));
        master.setTenantId(999L);
        master.setTableId(996001L);
        master.setOrderTime(LocalDateTime.now());
        orderService.save(master);

        mockMvc.perform(post("/api/dining/table/splitBill")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":996201,\"parts\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证主订单金额归零、状态为 SPLIT
        Orders masterAfter = orderService.getById(996201L);
        assertNotNull(masterAfter);
        assertEquals(0, masterAfter.getAmount().compareTo(BigDecimal.ZERO),
                "分账后主订单金额应为 0");
        assertEquals(Integer.valueOf(OrderStatus.SPLIT.getValue()), masterAfter.getStatus(),
                "分账后主订单状态应为 SPLIT");
        assertEquals(Integer.valueOf(3), masterAfter.getSplitCount(),
                "分账后主订单 splitCount 应为 3");
    }
}
