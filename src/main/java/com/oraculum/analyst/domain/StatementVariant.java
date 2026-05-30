package com.oraculum.analyst.domain;

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
}
