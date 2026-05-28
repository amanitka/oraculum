package com.oraculum.common.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LenientLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
    private static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public LenientLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (DateTimeParseException ignoredOffset) {
                try {
                    return LocalDateTime.parse(value, COMPACT_FORMATTER);
                } catch (DateTimeParseException fallback) {
                    throw context.weirdStringException(value, LocalDate.class, "Expected ISO_LOCAL_DATE, " +
                            "ISO_OFFSET_DATE_TIME, or yyyyMMdd'T'HHmmss");
                }
            }
        }
    }
}
