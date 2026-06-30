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
        assertEquals(6, OrderStatus.CANCELLED.getValue());
    }

    @Test
    void testUserStatus() {
        assertEquals(0, UserStatus.DISABLED.getValue());
        assertEquals(1, UserStatus.ENABLED.getValue());
    }
}
