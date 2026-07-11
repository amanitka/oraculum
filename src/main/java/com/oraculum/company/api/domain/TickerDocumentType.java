package com.oraculum.company.api.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum TickerDocumentType {
    FORM_8K("8K", TickerDocumentProvider.SEC),
    FORM_10K("10K", TickerDocumentProvider.SEC),
    EARNINGS_CALL("EARNINGS_CALL", TickerDocumentProvider.INTERNAL);

    private static final Map<String, TickerDocumentType> CODE_MAP = new HashMap<>();

    static {
        for (TickerDocumentType type : values()) {
            CODE_MAP.put(type.getCode().toLowerCase(), type);
        }
    }

    private final String code;
    private final TickerDocumentProvider provider;

    TickerDocumentType(String code, TickerDocumentProvider provider) {
        this.code = code;
        this.provider = provider;
    }

    public static Optional<TickerDocumentType> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(value.toLowerCase()));
    }
}
