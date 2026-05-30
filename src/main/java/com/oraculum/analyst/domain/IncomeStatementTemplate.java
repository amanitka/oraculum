package com.oraculum.analyst.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IncomeStatementTemplate {
    GENERAL("general"),
    BANK("bank"),
    INSURANCE("insurance");

    private final String value;

    IncomeStatementTemplate(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
