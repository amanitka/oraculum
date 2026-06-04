package com.oraculum.analyst.util;

import tools.jackson.databind.ObjectMapper;

public class JsonUtils {
    public static String toJson(ObjectMapper objectMapper, Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}