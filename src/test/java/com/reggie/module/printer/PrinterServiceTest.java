package com.reggie.module.printer;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.order.service.OrderService;
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
 * 打印模板渲染集成测试。
 * <p>打印已改为员工在浏览器手动调本地打印机（见 print-util.js），本类只覆盖
 * {@link PrinterTemplate} 的三类小票文本渲染，不再涉及终端入队/系统直连。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema-printer.sql", "classpath:schema-test-orders.sql"})
public class PrinterServiceTest {

    @Autowired
    private PrinterTemplate printerTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    private Orders testOrder;
    private List<OrderDetail> testDetails;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders", "print_task");
        BaseContext.setCurrentTenantId(1L);
        testOrder = new Orders();
        testOrder.setId(100L);
        testOrder.setNumber("TEST202607010001");
        testOrder.setStatus(2);
        testOrder.setUserId(1L);
        testOrder.setOrderTime(LocalDateTime.of(2026, 7, 1, 10, 30, 0));
        testOrder.setAmount(new BigDecimal("88.50"));
        testOrder.setRemark("少放辣");
        testOrder.setAddress("北京市朝阳区xx路100号");
        testOrder.setConsignee("张三");
        testOrder.setPhone("13800138000");
        testOrder.setTableId(5L);
        testOrder.setSource("EAT_IN");

        testDetails = new ArrayList<>();
        OrderDetail d1 = new OrderDetail();
        d1.setOrderId(testOrder.getId());
        d1.setName("宫保鸡丁");
        d1.setNumber(2);
        d1.setAmount(new BigDecimal("36.00"));
        d1.setDishFlavor("微辣");
        testDetails.add(d1);

        OrderDetail d2 = new OrderDetail();
        d2.setOrderId(testOrder.getId());
        d2.setName("米饭");
        d2.setNumber(2);
        d2.setAmount(new BigDecimal("6.00"));
        testDetails.add(d2);

        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
        orderService.save(testOrder);
    }

    @Test
    void testBillTemplate() {
        PrintJob job = printerTemplate.bill(testOrder, testDetails);

        assertNotNull(job);
        assertEquals("BILL", job.getPrintType());
        assertEquals(100L, job.getOrderId().longValue());
        assertNotNull(job.getLines());
        assertTrue(job.getLines().size() > 5);

        PrintLine first = job.getLines().get(0);
        assertEquals(PrintLine.LineType.TEXT, first.getType());
        assertEquals(PrintLine.Align.CENTER, first.getAlign());

        boolean hasTotal = job.getLines().stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("合计"));
        assertTrue(hasTotal);

        boolean hasQR = job.getLines().stream()
                .anyMatch(l -> l.getType() == PrintLine.LineType.QR);
        assertTrue(hasQR);
    }

    @Test
    void testKitchenTemplate() {
        PrintJob job = printerTemplate.kitchen(testOrder, testDetails);

        assertNotNull(job);
        assertEquals("KITCHEN", job.getPrintType());
        assertNotNull(job.getLines());

        boolean hasTable = job.getLines().stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("桌号"));
        assertTrue(hasTable);

        boolean hasRemark = job.getLines().stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("备注"));
        assertTrue(hasRemark);

        boolean hasDish = job.getLines().stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("宫保鸡丁"));
        assertTrue(hasDish);
    }

    @Test
    void testDeliveryTemplate() {
        PrintJob job = printerTemplate.delivery(testOrder, testDetails);

        assertNotNull(job);
        assertEquals("DELIVERY", job.getPrintType());
        assertNotNull(job.getLines());

        boolean hasAddress = job.getLines().stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("配送地址"));
        assertTrue(hasAddress);
    }

    @Test
    void testPlatformDeliveryTemplate() {
        // 平台外卖单：输出平台名称/平台单号/顾客/电话/备注
        testOrder.setPlatformType("MEITUAN");
        testOrder.setPlatformOrderId("MT202608240001");
        testOrder.setUserName("张三");
        PrintJob job = printerTemplate.delivery(testOrder, testDetails);

        assertNotNull(job);
        assertEquals("DELIVERY", job.getPrintType());

        String joined = job.getLines().stream()
                .map(PrintLine::getText)
                .filter(t -> t != null)
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(joined.contains("美团"), "应输出平台中文名: " + joined);
        assertTrue(joined.contains("MT202608240001"), "应输出平台单号: " + joined);
        assertTrue(joined.contains("顾客: 张三"), "应输出顾客信息: " + joined);
        assertTrue(joined.contains("电话: 13800138000"), "应输出顾客电话: " + joined);
        assertTrue(joined.contains("备注: 少放辣"), "应输出订单备注: " + joined);
        assertTrue(joined.contains("合计"), "应输出合计: " + joined);
    }
}
