package com.reggie.module.urgency.service;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.urgency.mapper.UrgencyMapper;
import com.reggie.module.urgency.service.impl.UrgencyServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C 端顾客催单逻辑单元测试。
 * <p>纯 Mock 测试（不连数据库），验证：归属/租户/状态校验、每日限次复用、成功后通知店长。</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerUrgencyTest {

    @Mock
    private UrgencyMapper urgencyMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UrgencyServiceImpl urgencyService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(999L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.setCurrentId(null);
        BaseContext.setCurrentTenantId(null);
    }

    /** 构造属于 userId、指定状态的订单 */
    private Orders order(Long id, Long userId, int status) {
        Orders order = new Orders();
        order.setId(id);
        order.setUserId(userId);
        order.setTenantId(999L);
        order.setStatus(status);
        order.setNumber("202609210001");
        order.setAmount(new BigDecimal("50.00"));
        return order;
    }

    /** mock 一位在职、有手机号的店长（notifyManagers 链式查询） */
    private void mockManager() {
        Employee manager = new Employee();
        manager.setId(9L);
        manager.setPhone("13800138000");
        List<Employee> managers = Collections.singletonList(manager);
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<Employee> chain = mock(
                com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(employeeService.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.isNotNull(any())).thenReturn(chain);
        when(chain.ne(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(managers);
    }

    @Test
    void trigger_ownPendingOrder_succeedsAndNotifiesManager() {
        when(orderMapper.selectById(100L)).thenReturn(order(100L, 1L, Orders.STATUS_ORDERED));
        when(urgencyMapper.countTodayByMember(eq(1L), eq(1L), any())).thenReturn(0);
        when(urgencyMapper.selectOne(any())).thenReturn(null);
        mockManager();

        R<Map<String, Object>> result = urgencyService.customerTrigger(100L, 1L);

        assertEquals(Integer.valueOf(1), result.getCode(), "本人待接单订单催单应成功");
        verify(urgencyMapper).insert(any());
        verify(notificationService).sendSimpleMessage(eq(1),
                eq(Collections.singletonList("13800138000")),
                eq("顾客催单提醒"), anyString());
    }

    @Test
    void trigger_deliveringOrder_succeeds() {
        when(orderMapper.selectById(101L)).thenReturn(order(101L, 1L, Orders.STATUS_DELIVERING));
        when(urgencyMapper.countTodayByMember(eq(1L), eq(1L), any())).thenReturn(0);
        when(urgencyMapper.selectOne(any())).thenReturn(null);
        mockManager();

        R<Map<String, Object>> result = urgencyService.customerTrigger(101L, 1L);
        assertEquals(Integer.valueOf(1), result.getCode(), "配送中订单也可催单");
    }

    @Test
    void trigger_othersOrder_rejected() {
        // 订单属于 2 号用户，1 号用户来催
        when(orderMapper.selectById(102L)).thenReturn(order(102L, 2L, Orders.STATUS_ORDERED));

        R<Map<String, Object>> result = urgencyService.customerTrigger(102L, 1L);

        assertEquals(Integer.valueOf(0), result.getCode(), "非本人订单应被拦截");
        verify(urgencyMapper, never()).insert(any());
        verify(notificationService, never()).sendSimpleMessage(
                org.mockito.ArgumentMatchers.anyInt(), any(), anyString(), anyString());
    }

    @Test
    void trigger_completedOrder_rejected() {
        when(orderMapper.selectById(103L)).thenReturn(order(103L, 1L, Orders.STATUS_COMPLETED));

        R<Map<String, Object>> result = urgencyService.customerTrigger(103L, 1L);

        assertEquals(Integer.valueOf(0), result.getCode(), "已完成订单不应催单");
        verify(urgencyMapper, never()).insert(any());
    }

    @Test
    void trigger_orderNotFound_rejected() {
        when(orderMapper.selectById(104L)).thenReturn(null);

        R<Map<String, Object>> result = urgencyService.customerTrigger(104L, 1L);
        assertEquals(Integer.valueOf(0), result.getCode(), "订单不存在应报错");
    }

    @Test
    void trigger_overDailyLimit_notNotifies() {
        when(orderMapper.selectById(105L)).thenReturn(order(105L, 1L, Orders.STATUS_ORDERED));
        // 今日已催 3 次，达上限
        when(urgencyMapper.countTodayByMember(eq(1L), eq(1L), any())).thenReturn(3);

        R<Map<String, Object>> result = urgencyService.customerTrigger(105L, 1L);

        assertEquals(Integer.valueOf(0), result.getCode(), "超出每日限次应失败");
        verify(urgencyMapper, never()).insert(any());
        verify(notificationService, never()).sendSimpleMessage(
                org.mockito.ArgumentMatchers.anyInt(), any(), anyString(), anyString());
    }
}
