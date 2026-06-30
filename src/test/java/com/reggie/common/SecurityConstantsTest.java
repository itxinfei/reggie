package com.reggie.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityConstantsTest {

    @Test
    void testConstantsExist() {
        assertNotNull(SecurityConstants.PASSWORD_MAX_LENGTH);
        assertNotNull(SecurityConstants.PASSWORD_MIN_LENGTH);
        assertNotNull(SecurityConstants.PHONE_PATTERN);
        assertTrue(SecurityConstants.PASSWORD_MIN_LENGTH > 0);
        assertTrue(SecurityConstants.PASSWORD_MAX_LENGTH >= SecurityConstants.PASSWORD_MIN_LENGTH);
    }
}
