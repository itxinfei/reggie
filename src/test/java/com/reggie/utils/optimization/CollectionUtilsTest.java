package com.reggie.utils.optimization;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {

    static class TestObj {
        private Long id;
        private String name;

        TestObj(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    @Test
    void testToList() {
        List<String> result = CollectionUtils.toList(Arrays.asList("a", "b", "c"));
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    void testMapToIds() {
        List<TestObj> list = Arrays.asList(new TestObj(1L, "A"), new TestObj(2L, "B"));
        List<Long> ids = CollectionUtils.mapToIds(list, TestObj::getId);
        assertEquals(2, ids.size());
        assertEquals(1L, ids.get(0));
        assertEquals(2L, ids.get(1));
    }

    @Test
    void testMapTo() {
        List<TestObj> list = Arrays.asList(new TestObj(1L, "A"), new TestObj(2L, "B"));
        List<String> names = CollectionUtils.mapTo(list, TestObj::getName);
        assertEquals(2, names.size());
        assertEquals("A", names.get(0));
        assertEquals("B", names.get(1));
    }

    @Test
    void testEmptyList() {
        assertTrue(CollectionUtils.mapToIds(Collections.emptyList(), TestObj::getId).isEmpty());
        assertTrue(CollectionUtils.mapTo(Collections.emptyList(), obj -> obj.toString()).isEmpty());
    }
}
