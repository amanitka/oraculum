package com.oraculum.company.api.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatementVariant {
    ANNUAL("annual"),
    QUARTERLY("quarterly"),
    TTM("ttm");

    private final String value;

    StatementVariant(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StatementVariant fromValue(String value) {
        for (StatementVariant t : StatementVariant.values()) {
            if (t.value.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        return null;
    }
}
