package com.oraculum.company.api.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum TickerDocumentType {
    SEC_8K("SEC_8K", "8-K", TickerDocumentProvider.SEC),
    SEC_10K("SEC_10K", "10-K", TickerDocumentProvider.SEC),
    SEC_10Q("SEC_10Q", "10-Q", TickerDocumentProvider.SEC);

    private static final Map<String, TickerDocumentType> CODE_MAP = new HashMap<>();
    private static final Map<String, TickerDocumentType> SEC_FORM_MAP = new HashMap<>();

    static {
        for (TickerDocumentType type : values()) {
            CODE_MAP.put(type.getCode().toLowerCase(), type);
            if (type.getSecFormName() != null) {
                SEC_FORM_MAP.put(type.getSecFormName(), type);
            }
        }
    }

    private final String code;
    private final String secFormName;
    private final TickerDocumentProvider provider;

    TickerDocumentType(String code, String secFormName, TickerDocumentProvider provider) {
        this.code = code;
        this.secFormName = secFormName;
        this.provider = provider;
    }

    public static Optional<TickerDocumentType> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(value.toLowerCase()));
    }

    public static Optional<TickerDocumentType> fromSecFormName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SEC_FORM_MAP.get(value));
    }
}
