package com.reggie.module.delivery.service.impl;

import com.reggie.common.BaseContext;
import com.reggie.module.delivery.dto.RiderTaskVO;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.tenant.service.TenantService;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P2-3 号码保护：骑手任务 VO 手机号分级披露单测。
 * 未持单（大厅/列表）脱敏 138****5678；已归属当前骑手才下发真实号。
 */
@ExtendWith(MockitoExtension.class)
public class RiderTaskPhoneMaskTest {

    private static final Long RIDER_ID = 5001L;
    private static final String PHONE = "13812345678";
    private static final String MASKED = "138****5678";

    @Mock
    private OrderService orderService;
    @Mock
    private OrderDetailService orderDetailService;
    @Mock
    private TenantService tenantService;
    @Mock
    private StoreService storeService;
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private DeliveryTrackingService deliveryTrackingService;

    @InjectMocks
    private RiderTaskQueryServiceImpl service;

    @BeforeEach
    public void setUp() {
        BaseContext.setCurrentId(RIDER_ID);
        // 其余依赖默认返回 null/空，toVO 中均有判空兜底
        when(orderDetailService.list(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    public void tearDown() {
        BaseContext.remove();
    }

    private Orders order(Long riderId) {
        Orders o = new Orders();
        o.setId(7001L);
        o.setStatus(Orders.STATUS_ORDERED);
        o.setRiderId(riderId);
        o.setPhone(PHONE);
        o.setTenantId(1L);
        o.setAddressBookId(null);
        return o;
    }

    @Test
    public void hall_phoneMasked() {
        when(orderService.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(order(null))));
        List<RiderTaskVO> list = service.listHall();
        assertEquals(1, list.size());
        assertEquals(MASKED, list.get(0).getPhone());
    }

    @Test
    public void mine_phoneRevealed() {
        when(orderService.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(order(RIDER_ID))));
        List<RiderTaskVO> list = service.listMine(RIDER_ID, null);
        assertEquals(1, list.size());
        assertEquals(PHONE, list.get(0).getPhone());
    }

    @Test
    public void detailInHall_phoneMasked() {
        when(orderService.getById(7001L)).thenReturn(order(null));
        RiderTaskVO vo = service.getDetail(7001L, RIDER_ID);
        assertEquals(MASKED, vo.getPhone());
    }

    @Test
    public void detailMine_phoneRevealed() {
        when(orderService.getById(7001L)).thenReturn(order(RIDER_ID));
        RiderTaskVO vo = service.getDetail(7001L, RIDER_ID);
        assertEquals(PHONE, vo.getPhone());
    }
}
