package com.oraculum.util;

public final class NumericUtil {

    private NumericUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Double stringToDouble(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals(".")) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
