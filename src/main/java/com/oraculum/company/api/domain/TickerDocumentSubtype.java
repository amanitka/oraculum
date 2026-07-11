package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum TickerDocumentSubtype {
    RF("RF"),
    MD("MD"),
    EX99_1("EX99_1"),
    TRANSCRIPT("TRANSCRIPT");

    private final String code;

    TickerDocumentSubtype(String code) {
        this.code = code;
    }

}
