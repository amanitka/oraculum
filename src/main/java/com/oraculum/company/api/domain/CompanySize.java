package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum CompanySize {
    LARGE("Large"),
    MID("Mid"),
    SMALL("Small"),
    MICRO("Micro");

    private final String displayName;

    CompanySize(String displayName) {
        this.displayName = displayName;
    }

    public static CompanySize fromCode(String code) {
        if (code == null) return null;
        try {
            return CompanySize.valueOf(code.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
