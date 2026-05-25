package com.oraculum.util;

public final class TimeUtil {

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static int getDurationMs(long startTimeNano) {
        return (int) ((System.nanoTime() - startTimeNano) / 1_000_000);
    }
}
