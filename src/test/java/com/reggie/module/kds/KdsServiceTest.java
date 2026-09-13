package com.reggie.module.kds;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.kds.service.KitchenTicketService;
import com.reggie.module.kds.vo.KitchenBoardVO;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 后厨出餐 KDS 模块测试。
 * <p>覆盖：从已下单订单幂等拉单 → 待制作/制作中/待取餐/完成全流转 → 非法流转拒绝 →
 * 取消与加急 → 大屏分栏与统计。依赖主 schema.sql 的 orders/order_detail。</p>
 *
 * @author reggie
 * @since 2026-09-13
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-kds.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class KdsServiceTest {

    @Autowired
    private KitchenTicketService kitchenTicketService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("kitchen_ticket", "order_detail", "orders");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    /** 构造一个订单（orders.id 测试库无自增，需手动指定） */
    private Orders buildOrder(long id, int status, String source) {
        Orders o = new Orders();
        o.setId(id);
        o.setNumber("K" + id);
        o.setStatus(status);
        o.setSource(source);
        o.setTableName("A" + id);
        o.setCustomerCount(2);
        o.setAmount(new BigDecimal("58.00"));
        o.setRemark("不要香菜");
        o.setTenantId(1L);
        orderMapper.insert(o);
        return o;
    }

    private void addDetail(long orderId, String name, String flavor, int num) {
        OrderDetail d = new OrderDetail();
        d.setOrderId(orderId);
        d.setName(name);
        d.setDishFlavor(flavor);
        d.setNumber(num);
        d.setAmount(new BigDecimal("29.00"));
        d.setTenantId(1L);
        orderDetailMapper.insert(d);
    }

    private List<KitchenTicket> allTickets() {
        return kitchenTicketService.list();
    }

    @Test
    void testPullPendingOrdersIdempotent() {
        buildOrder(9101, Orders.STATUS_ORDERED, "EAT_IN");
        addDetail(9101, "鱼香肉丝", "微辣", 2);
        addDetail(9101, "米饭", null, 2);
        buildOrder(9102, Orders.STATUS_ORDERED, "OUTSIDE");
        addDetail(9102, "宫保鸡丁", null, 1);

        int first = kitchenTicketService.pullPendingOrders();
        assertEquals(2, first);
        List<KitchenTicket> tickets = allTickets();
        assertEquals(2, tickets.size());
        KitchenTicket t = tickets.stream().filter(x -> x.getOrderId() == 9101L).findFirst().orElse(null);
        assertNotNull(t);
        assertEquals(KitchenTicket.STATUS_PENDING, t.getStatus());
        assertEquals("K9101", t.getOrderNo());
        assertEquals("EAT_IN", t.getOrderType());
        assertNotNull(t.getReceiveTime());
        // 菜品摘要含菜名与数量
        assertTrue(t.getDishSummary().contains("鱼香肉丝"));
        assertTrue(t.getDishSummary().contains("x2"));

        // 再次拉取不重复建单
        int second = kitchenTicketService.pullPendingOrders();
        assertEquals(0, second);
        assertEquals(2, allTickets().size());
    }

    @Test
    void testPullSkipsUnpaidOrder() {
        // 待付款订单不应进入后厨
        buildOrder(9103, Orders.STATUS_PENDING_PAY, "OUTSIDE");
        addDetail(9103, "凉菜", null, 1);
        assertEquals(0, kitchenTicketService.pullPendingOrders());
        assertEquals(0, allTickets().size());
    }

    @Test
    void testFullFlowStartReadyFinish() {
        buildOrder(9201, Orders.STATUS_ORDERED, "EAT_IN");
        addDetail(9201, "红烧肉", null, 1);
        kitchenTicketService.pullPendingOrders();
        KitchenTicket t = allTickets().get(0);

        // 待制作 → 制作中
        KitchenTicket cooking = kitchenTicketService.startCook(t.getId());
        assertEquals(KitchenTicket.STATUS_COOKING, cooking.getStatus());
        assertNotNull(cooking.getCookStartTime());

        // 制作中 → 待取餐（叫号），记录制作耗时
        KitchenTicket ready = kitchenTicketService.markReady(t.getId());
        assertEquals(KitchenTicket.STATUS_READY, ready.getStatus());
        assertNotNull(ready.getReadyTime());
        assertNotNull(ready.getCookDurationSeconds());

        // 待取餐 → 已完成
        KitchenTicket finished = kitchenTicketService.finish(t.getId());
        assertEquals(KitchenTicket.STATUS_FINISHED, finished.getStatus());
        assertNotNull(finished.getFinishTime());
    }

    @Test
    void testIllegalTransitionsRejected() {
        buildOrder(9301, Orders.STATUS_ORDERED, "OUTSIDE");
        addDetail(9301, "牛肉面", null, 1);
        kitchenTicketService.pullPendingOrders();
        Long id = allTickets().get(0).getId();

        // 待制作不能直接叫号/完成
        assertThrows(CustomException.class, () -> kitchenTicketService.markReady(id));
        assertThrows(CustomException.class, () -> kitchenTicketService.finish(id));

        kitchenTicketService.startCook(id);
        // 制作中不能直接完成（必须先叫号）、不能重复开始
        assertThrows(CustomException.class, () -> kitchenTicketService.finish(id));
        assertThrows(CustomException.class, () -> kitchenTicketService.startCook(id));

        kitchenTicketService.markReady(id);
        // 待取餐不能再开始/叫号
        assertThrows(CustomException.class, () -> kitchenTicketService.startCook(id));
        assertThrows(CustomException.class, () -> kitchenTicketService.markReady(id));
    }

    @Test
    void testCancelRules() {
        buildOrder(9401, Orders.STATUS_ORDERED, "OUTSIDE");
        addDetail(9401, "炒饭", null, 1);
        kitchenTicketService.pullPendingOrders();
        Long id = allTickets().get(0).getId();

        KitchenTicket cancelled = kitchenTicketService.cancel(id);
        assertEquals(KitchenTicket.STATUS_CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelTime());
        // 已取消不能再取消/开始
        assertThrows(CustomException.class, () -> kitchenTicketService.cancel(id));
        assertThrows(CustomException.class, () -> kitchenTicketService.startCook(id));
    }

    @Test
    void testToggleUrgent() {
        buildOrder(9501, Orders.STATUS_ORDERED, "EAT_IN");
        addDetail(9501, "水煮鱼", "加麻", 1);
        kitchenTicketService.pullPendingOrders();
        Long id = allTickets().get(0).getId();

        assertEquals(KitchenTicket.URGENT_NO, allTickets().get(0).getUrgent());
        KitchenTicket u = kitchenTicketService.toggleUrgent(id);
        assertEquals(KitchenTicket.URGENT_YES, u.getUrgent());
        kitchenTicketService.toggleUrgent(id);
        assertEquals(KitchenTicket.URGENT_NO, kitchenTicketService.getById(id).getUrgent());
    }

    @Test
    void testBoardGroupingAndStats() {
        // 工单1：完整走到已完成
        buildOrder(9601, Orders.STATUS_ORDERED, "OUTSIDE");
        addDetail(9601, "套餐A", null, 1);
        // 工单2：走到制作中
        buildOrder(9602, Orders.STATUS_ORDERED, "EAT_IN");
        addDetail(9602, "套餐B", null, 1);
        // 工单3：停在待制作并加急
        buildOrder(9603, Orders.STATUS_ORDERED, "EAT_IN");
        addDetail(9603, "套餐C", null, 1);

        // 用看板自动拉取
        KitchenBoardVO pulled = kitchenTicketService.getBoard(true);
        assertEquals(3, pulled.getPendingCount());
        assertEquals(3, pulled.getPulledCount());

        List<KitchenTicket> tickets = allTickets();
        KitchenTicket t1 = findByOrder(tickets, 9601L);
        KitchenTicket t2 = findByOrder(tickets, 9602L);
        KitchenTicket t3 = findByOrder(tickets, 9603L);

        kitchenTicketService.startCook(t1.getId());
        kitchenTicketService.markReady(t1.getId());
        kitchenTicketService.finish(t1.getId());

        kitchenTicketService.startCook(t2.getId());

        kitchenTicketService.toggleUrgent(t3.getId());

        KitchenBoardVO board = kitchenTicketService.getBoard(false);
        assertEquals(1, board.getPendingCount());   // t3
        assertEquals(1, board.getCookingCount());   // t2
        assertEquals(0, board.getReadyCount());
        assertEquals(1, board.getFinishedCount());  // t1 今日完成
        assertEquals(1, board.getUrgentCount());    // t3 加急
        // 待制作列携带菜品明细
        assertEquals(1, board.getPending().get(0).getDetails().size());
        assertNotNull(board.getPending().get(0).getWaitSeconds());
        // t1 有制作耗时，平均值非负
        assertTrue(board.getAvgCookSeconds() >= 0);
    }

    private KitchenTicket findByOrder(List<KitchenTicket> tickets, long orderId) {
        return tickets.stream().filter(x -> x.getOrderId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalStateException("工单不存在 orderId=" + orderId));
    }
}
