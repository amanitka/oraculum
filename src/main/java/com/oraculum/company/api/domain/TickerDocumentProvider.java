package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum TickerDocumentProvider {
    SEC("SEC"),
    INTERNAL("INTERNAL");

    private final String code;

    TickerDocumentProvider(String code) {
        this.code = code;
    }
}
