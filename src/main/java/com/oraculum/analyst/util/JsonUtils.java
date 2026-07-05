package com.oraculum.analyst.util;

import tools.jackson.databind.json.JsonMapper;

public class JsonUtils {
    public static String toJson(JsonMapper jsonMapper, Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
