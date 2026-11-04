package com.oraculum.util;

import java.util.List;
import java.util.Objects;

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

    public static Float round1(double value) {
        return (float) (Math.round(value * 10.0) / 10.0);
    }

    public static Float round2(double value) {
        return (float) (Math.round(value * 100.0) / 100.0);
    }

    public static double medianDouble(List<Double> values, double defaultValue) {
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        List<Double> sorted = values.stream().filter(Objects::nonNull).sorted().toList();
        if (sorted.isEmpty()) {
            return defaultValue;
        }
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(mid)
                : (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }

    public static Float medianFloat(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Float> sorted = values.stream().filter(Objects::nonNull).sorted().toList();
        if (sorted.isEmpty()) {
            return null;
        }
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(mid)
                : round1((sorted.get(mid - 1) + sorted.get(mid)) / 2.0);
    }

}
