package com.reggie.module.urgency.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.sys.service.SystemConfigService;
import com.reggie.module.urgency.mapper.UrgencyMapper;
import com.reggie.module.urgency.service.impl.UrgencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 未接单扫描告警逻辑单元测试
 * <p>纯 Mock 测试（不连数据库/Redis），验证漏单预警核心行为：
 * 分级判定、每订单一次性通知去重、通知店长。默认阈值：golden=3 / alarm=10 / missed=9。</p>
 */
@ExtendWith(MockitoExtension.class)
class UrgencyUnacceptedScanTest {

    @Mock
    private UrgencyMapper urgencyMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private DiningTableService diningTableService;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private UrgencyServiceImpl urgencyService;

    @BeforeEach
    void setUp() {
        // 共享 stubbing 可能不被所有用例用到，使用 lenient 避免严格模式误报
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        // 系统配置缺省 → 走类常量默认值（golden=3/alarm=10/missed=9）
        lenient().when(systemConfigService.getConfigOrDefault(anyString(), any())).thenReturn(null);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
    }

    /** 构造指定等待分钟的待接单订单 */
    private Orders order(Long id, String no, int waitMinutesAgo) {
        Orders order = new Orders();
        order.setId(id);
        order.setNumber(no);
        order.setStatus(Orders.STATUS_ORDERED);
        order.setOrderTime(LocalDateTime.now().minusMinutes(waitMinutesAgo));
        order.setCreateTime(LocalDateTime.now().minusMinutes(waitMinutesAgo));
        order.setTenantId(1L);
        order.setUserName("测试用户");
        order.setPhone("13800000000");
        order.setAmount(new BigDecimal("88.00"));
        order.setSource("TAKEOUT");
        return order;
    }

    private void mockPendingOrders(List<Orders> orders) {
        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);
    }

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

    // ==================== 分级与通知 ====================

    @Test
    void missOrder_overMissedThreshold_triggersMissedAlertOnce() {
        // 等待 10 分钟 > 漏单阈值 9 → 漏单告警
        mockPendingOrders(Collections.singletonList(order(1L, "202609010001", 10)));
        mockManager();
        when(setOperations.add(anyString(), any())).thenReturn(1L, 0L);

        int first = urgencyService.scanUnacceptedAndAlert(1L);
        assertEquals(1, first, "首次扫描应触发漏单告警");
        verify(notificationService).sendSimpleMessage(eq(1), eq(Collections.singletonList("13800138000")),
                anyString(), org.mockito.ArgumentMatchers.contains("漏单"));

        int second = urgencyService.scanUnacceptedAndAlert(1L);
        assertEquals(0, second, "第二次扫描同一订单不应重复告警（Redis 去重）");
    }

    @Test
    void warningOrder_overGoldenUnderMissed_triggersGoldenAlertOnce() {
        // 等待 5 分钟：>= golden(3) 且 < missed(9) → 预警通知店长
        mockPendingOrders(Collections.singletonList(order(2L, "202609010002", 5)));
        mockManager();
        when(setOperations.add(anyString(), any())).thenReturn(1L, 0L);

        assertEquals(1, urgencyService.scanUnacceptedAndAlert(1L), "超黄金时长应触发预警通知");
        verify(notificationService).sendSimpleMessage(eq(1), eq(Collections.singletonList("13800138000")),
                anyString(), org.mockito.ArgumentMatchers.contains("超时"));

        assertEquals(0, urgencyService.scanUnacceptedAndAlert(1L), "同一订单不重复通知");
    }

    @Test
    void normalOrder_underGolden_neverNotifies() {
        // 等待 1 分钟 < golden(3) → 正常，不通知
        mockPendingOrders(Collections.singletonList(order(3L, "202609010003", 1)));

        assertEquals(0, urgencyService.scanUnacceptedAndAlert(1L), "黄金时长内不应告警");
        verify(notificationService, never()).sendSimpleMessage(any(), any(), anyString(), anyString());
    }

    @Test
    void missedOrder_upgradesOverGoldenAlert() {
        // 同一订单先以预警告警，再超漏单阈值升级 → 两次不同级别告警各触发一次
        mockPendingOrders(Collections.singletonList(order(4L, "202609010004", 10)));
        mockManager();
        when(setOperations.add("urgency:alert:missed:1", "4")).thenReturn(1L);

        assertEquals(1, urgencyService.scanUnacceptedAndAlert(1L), "漏单订单仅触发漏单升级告警");
        verify(notificationService).sendSimpleMessage(eq(1), eq(Collections.singletonList("13800138000")),
                anyString(), org.mockito.ArgumentMatchers.contains("漏单"));
        // 不再重复触发黄金预警（漏单级别已覆盖）
        verify(notificationService, never()).sendSimpleMessage(eq(1), eq(Collections.singletonList("13800138000")),
                anyString(), org.mockito.ArgumentMatchers.contains("超时"));
    }
}
