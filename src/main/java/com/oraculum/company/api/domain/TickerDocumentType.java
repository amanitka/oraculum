package com.oraculum.company.api.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum TickerDocumentType {
    SEC_8K("SEC_8K", TickerDocumentProvider.SEC),
    SEC_10K("SEC_10K", TickerDocumentProvider.SEC),
    SEC_10Q("SEC_10Q", TickerDocumentProvider.SEC);

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
