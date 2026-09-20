package com.reggie.module.ai.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具入参 JSON Schema 拼装小助手（JDK 1.8 语法，避免各工具重复样板）。
 *
 * @author reggie
 * @since 2026-09-20
 */
public final class ToolSchemas {

    private ToolSchemas() {
    }

    /** object 类型 schema */
    public static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", new ArrayList<>(required));
        }
        return schema;
    }

    /** string 类型属性 */
    public static Map<String, Object> stringType(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("description", description);
        return m;
    }

    /** integer 类型属性 */
    public static Map<String, Object> integerType(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "integer");
        m.put("description", description);
        return m;
    }
}
