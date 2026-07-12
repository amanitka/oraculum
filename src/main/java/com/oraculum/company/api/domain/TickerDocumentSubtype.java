package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum TickerDocumentSubtype {
    SEC_RF("SEC_RF"),
    SEC_MD("SEC_MD"),
    SEC_EX99_1("SEC_EX99_1"),
    TRANSCRIPT("TRANSCRIPT");

    private final String code;

    TickerDocumentSubtype(String code) {
        this.code = code;
    }
}
