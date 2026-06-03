package com.oraculum.company.api.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatementTemplate {
    GENERAL("general"),
    BANK("bank"),
    INSURANCE("insurance");

    private final String value;

    StatementTemplate(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
