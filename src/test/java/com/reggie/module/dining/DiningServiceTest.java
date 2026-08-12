package com.reggie.module.dining;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-dining.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DiningServiceTest {

    @Autowired
    private TableAreaService tableAreaService;

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testAreaCrud() {
        TableArea area = new TableArea();
        area.setTenantId(1L);
        area.setName("大厅");
        area.setSort(1);
        tableAreaService.save(area);
        assertNotNull(area.getId());

        TableArea found = tableAreaService.getById(area.getId());
        assertEquals("大厅", found.getName());

        found.setName("包间");
        tableAreaService.updateById(found);
        TableArea updated = tableAreaService.getById(area.getId());
        assertEquals("包间", updated.getName());

        tableAreaService.removeById(area.getId());
        assertNull(tableAreaService.getById(area.getId()));
    }

    @Test
    void testTableCrud() {
        TableArea area = new TableArea();
        area.setTenantId(1L);
        area.setName("大厅");
        area.setSort(1);
        tableAreaService.save(area);

        DiningTable table = new DiningTable();
        table.setTenantId(1L);
        table.setAreaId(area.getId());
        table.setName("A01");
        table.setSeatCount(4);
        table.setStatus("FREE");
        table.setMinAmount(new BigDecimal("0.00"));
        table.setSort(1);
        diningTableService.save(table);
        assertNotNull(table.getId());

        DiningTable found = diningTableService.getById(table.getId());
        assertEquals("A01", found.getName());
        assertEquals("FREE", found.getStatus());

        found.setName("A02");
        diningTableService.updateById(found);
        DiningTable updated = diningTableService.getById(table.getId());
        assertEquals("A02", updated.getName());

        diningTableService.removeById(table.getId());
        assertNull(diningTableService.getById(table.getId()));
    }

    @Test
    void testTableStatusChange() {
        DiningTable table = new DiningTable();
        table.setTenantId(1L);
        table.setName("B01");
        table.setSeatCount(6);
        table.setStatus("FREE");
        diningTableService.save(table);

        diningTableService.changeStatus(table.getId(), "OCCUPIED");
        assertEquals("OCCUPIED", diningTableService.getById(table.getId()).getStatus());

        diningTableService.changeStatus(table.getId(), "CLEANING");
        assertEquals("CLEANING", diningTableService.getById(table.getId()).getStatus());

        diningTableService.changeStatus(table.getId(), "FREE");
        assertEquals("FREE", diningTableService.getById(table.getId()).getStatus());
    }

    @Test
    void testQueueTakeNumber() {
        QueueRecord record = queueService.takeNumber(4, "13800138001");
        assertNotNull(record.getId());
        assertNotNull(record.getQueueNo());
        assertTrue(record.getQueueNo().length() >= 8);
        assertEquals("WAITING", record.getStatus());
        assertEquals("13800138001", record.getPhone());
        assertEquals(Integer.valueOf(4), record.getSeatCount());

        QueueRecord second = queueService.takeNumber(2, "13800138002");
        assertNotNull(second.getQueueNo());
        assertNotEquals(record.getQueueNo(), second.getQueueNo());
    }

    @Test
    void testQueueCallNext() {
        queueService.takeNumber(4, "13800138001");
        queueService.takeNumber(4, "13800138002");
        queueService.takeNumber(2, "13800138003");

        QueueRecord called = queueService.callNext(4);
        assertNotNull(called);
        assertEquals("CALLED", called.getStatus());
        assertEquals("13800138001", called.getPhone());

        QueueRecord nextCall = queueService.callNext(4);
        assertEquals("13800138002", nextCall.getPhone());
    }

    @Test
    void testCreateReservation() {
        TableArea area = new TableArea();
        area.setTenantId(1L);
        area.setName("大厅");
        tableAreaService.save(area);

        DiningTable table = new DiningTable();
        table.setTenantId(1L);
        table.setAreaId(area.getId());
        table.setName("A01");
        table.setSeatCount(4);
        table.setStatus("FREE");
        diningTableService.save(table);

        LocalDateTime reservedTime = LocalDateTime.now().plusHours(2);
        Reservation r = reservationService.createReservation("张三", "13800138001", reservedTime, 4, table.getId(), "靠窗");
        assertNotNull(r.getId());
        assertEquals("张三", r.getCustomerName());
        assertEquals("PENDING", r.getStatus());
        assertEquals(table.getId(), r.getTableId());
    }

    @Test
    void testConfirmCancelReservation() {
        LocalDateTime reservedTime = LocalDateTime.now().plusHours(2);
        Reservation r = reservationService.createReservation("李四", "13800138002", reservedTime, 2, null, null);
        assertNotNull(r.getId());

        reservationService.confirmReservation(r.getId());
        assertEquals("CONFIRMED", reservationService.getById(r.getId()).getStatus());

        reservationService.cancelReservation(r.getId());
        assertEquals("CANCELLED", reservationService.getById(r.getId()).getStatus());
    }
}

