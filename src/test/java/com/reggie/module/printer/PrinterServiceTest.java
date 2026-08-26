package com.reggie.module.printer;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.printer.adapter.WindowsSystemPrinterAdapter;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.module.printer.service.PrinterConfigService;
import com.reggie.module.printer.service.PrinterService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.print.PrintService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = {"classpath:schema-printer.sql", "classpath:schema-test-orders.sql"})
public class PrinterServiceTest {

    @Autowired
    private PrinterTemplate printerTemplate;

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private WindowsSystemPrinterAdapter windowsSystemPrinterAdapter;

    @Autowired
    private TestDatabaseCleaner cleaner;

    private Orders testOrder;
    private List<OrderDetail> testDetails;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders", "printer_log", "printer_template", "printer_config");
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
    void testPrintOrder() {
        for (OrderDetail d : testDetails) {
            orderDetailService.save(d);
        }

        PrinterConfig printer = new PrinterConfig();
        printer.setTenantId(1L);
        printer.setName("测试打印机");
        printer.setType("USB");
        printer.setBrand("GPRINTER");
        printer.setDeviceId("SN001");
        printer.setPrintType("BILL");
        printer.setStatus(1);
        printerConfigService.save(printer);

        printerService.printOrder(testOrder.getId(), "BILL");
    }

    @Test
    void testListSystemPrinters() {
        List<PrintService> printers = windowsSystemPrinterAdapter.listSystemPrinters();
        assertNotNull(printers);
    }

    @Test
    void testWindowsAdapterQueryStatusWithNullName() {
        PrinterConfig config = new PrinterConfig();
        config.setDeviceId(null);

        PrinterStatus status = windowsSystemPrinterAdapter.queryStatus(config);
        assertNotNull(status);
        assertFalse(status.isOnline());
        assertEquals("打印机名称为空", status.getDetail());
    }

    @Test
    void testWindowsAdapterTestConnectionWithNullName() {
        PrinterConfig config = new PrinterConfig();
        config.setDeviceId(null);

        boolean result = windowsSystemPrinterAdapter.testConnection(config);
        assertFalse(result);
    }

    @Test
    void testWindowsAdapterQueryStatusWithNonExistentPrinter() {
        PrinterConfig config = new PrinterConfig();
        config.setDeviceId("不存在的打印机12345");

        PrinterStatus status = windowsSystemPrinterAdapter.queryStatus(config);
        assertNotNull(status);
        assertFalse(status.isOnline());
        assertEquals("打印机不存在", status.getDetail());
    }

    @Test
    void testWindowsAdapterTestConnectionWithNonExistentPrinter() {
        PrinterConfig config = new PrinterConfig();
        config.setDeviceId("不存在的打印机12345");

        boolean result = windowsSystemPrinterAdapter.testConnection(config);
        assertFalse(result);
    }
}

