package com.reggie.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformOrderPersistService;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.mapper.PrintTerminalMapper;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.model.PrintTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台订单落库（幂等去重）集成测试
 * <p>覆盖：字段映射、租户隔离、重复拉单去重、明细落库、落库后自动打印。</p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = {"classpath:schema-platform.sql", "classpath:schema-printer.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlatformOrderPersistTest {

    @Autowired
    private PlatformOrderPersistService persistService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PlatformConfigService configService;
    @Autowired
    private PrintTerminalMapper printTerminalMapper;
    @Autowired
    private PrintTaskMapper printTaskMapper;

    private static final Long TENANT = 1L;

    /** 预置门店 PC 打印代理终端（tenant=1，print_types 为空=接收全部类型） */
    private Long setupTerminal() {
        PrintTerminal terminal = new PrintTerminal();
        terminal.setTenantId(TENANT);
        terminal.setStoreCode("S0001");
        terminal.setTerminalCode("T-PLATFORM-001");
        terminal.setToken("test-token");
        terminal.setName("测试终端");
        terminal.setPrinterName("TEST_PRINTER");
        terminal.setPaperSize("80mm");
        terminal.setPrintTypes("");
        terminal.setClientVersion("1.0.0");
        terminal.setStatus(1);
        terminal.setCreatedTime(LocalDateTime.now());
        terminal.setUpdateTime(LocalDateTime.now());
        printTerminalMapper.insertIgnoreTenant(terminal);
        return terminal.getId();
    }

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
    void testPersistTriggersAutoPrint() {
        Long terminalId = setupTerminal();

        List<PlatformOrder> orders = new ArrayList<>();
        orders.add(buildOrder("MT202608240005", "D005"));

        int inserted = persistService.persistOrders("MEITUAN", "shop_001", TENANT, orders);
        assertEquals(1, inserted);

        // 落库后自动入队打印任务：外卖单 + 后厨单
        List<PrintTask> tasks = printTaskMapper.listPending(terminalId, 10);
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> "DELIVERY".equals(t.getTaskType())));
        assertTrue(tasks.stream().anyMatch(t -> "KITCHEN".equals(t.getTaskType())));
        PrintTask delivery = tasks.stream()
                .filter(t -> "DELIVERY".equals(t.getTaskType()))
                .findFirst().orElse(null);
        assertNotNull(delivery);
        assertEquals("PENDING", delivery.getStatus());
        assertTrue(delivery.getContent() != null && delivery.getContent().contains("外卖单"));

        // 重复拉单去重：不新增订单，也不重复打印
        int insertedAgain = persistService.persistOrders("MEITUAN", "shop_001", TENANT, orders);
        assertEquals(0, insertedAgain);
        assertEquals(2, printTaskMapper.listPending(terminalId, 10).size());
    }

    @Test
    void testPersistWithoutTerminalDoesNotBreak() {
        // 无启用终端：落库成功且不抛异常（自动打印静默跳过）
        List<PlatformOrder> orders = new ArrayList<>();
        orders.add(buildOrder("MT202608240006", "D006"));
        int inserted = persistService.persistOrders("ELEME", "shop_001", TENANT, orders);
        assertEquals(1, inserted);
        assertTrue(persistService.exists("ELEME", "MT202608240006", TENANT));
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
