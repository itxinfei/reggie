package com.reggie.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void testSuccessHasTimestampAndRequestId() {
        R<String> r = R.success("test");
        assertNotNull(r.getTimestamp());
        assertNotNull(r.getRequestId());
        assertTrue(r.getTimestamp() > 0);
        assertFalse(r.getRequestId().isEmpty());
    }

    @Test
    void testErrorHasTimestampAndRequestId() {
        R<String> r = R.error("error");
        assertNotNull(r.getTimestamp());
        assertNotNull(r.getRequestId());
        assertTrue(r.getTimestamp() > 0);
        assertFalse(r.getRequestId().isEmpty());
    }

    @Test
    void testRequestIdIsUnique() {
        R<String> r1 = R.success("test1");
        R<String> r2 = R.success("test2");
        assertNotEquals(r1.getRequestId(), r2.getRequestId());
    }
}
