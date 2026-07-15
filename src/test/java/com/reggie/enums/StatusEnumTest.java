package com.reggie.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusEnumTest {

    @Test
    void testDishStatus() {
        assertEquals(0, DishStatus.DISABLED.getValue());
        assertEquals(1, DishStatus.ENABLED.getValue());
    }

    @Test
    void testOrderStatus() {
        assertEquals(1, OrderStatus.PENDING_PAYMENT.getValue());
        assertEquals(5, OrderStatus.CANCELLED.getValue());
        assertEquals(6, OrderStatus.REFUNDED.getValue());
        assertEquals(OrderStatus.ORDERED, OrderStatus.fromCode(2));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromCode(4));
    }

    @Test
    void testUserStatus() {
        assertEquals(0, UserStatus.DISABLED.getValue());
        assertEquals(1, UserStatus.ENABLED.getValue());
    }
}
