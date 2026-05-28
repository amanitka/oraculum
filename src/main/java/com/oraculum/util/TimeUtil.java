package com.oraculum.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class TimeUtil {

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static OffsetDateTime toOffsetDateTime(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }
}
