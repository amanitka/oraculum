package com.oraculum.company.api.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static StatementTemplate fromValue(String value) {
        for (StatementTemplate t : StatementTemplate.values()) {
            if (t.value.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        return null;
    }
}
