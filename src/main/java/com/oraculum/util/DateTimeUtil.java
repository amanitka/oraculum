package com.oraculum.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");

    private DateTimeUtil() {
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

    public static String toIsoDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return ISO_LOCAL_DATE_FORMATTER.format(value);
    }

    public static String toIsoCompactDateTime(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return ISO_COMPACT_DATETIME_FORMATTER.format(value);
    }
}
