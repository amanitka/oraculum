package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum TickerDocumentSubtype {
    ITEM_1A("ITEM_1A"),
    ITEM_7("ITEM_7"),
    EX99_1("EX99_1"),
    TRANSCRIPT("TRANSCRIPT");

    private final String code;

    TickerDocumentSubtype(String code) {
        this.code = code;
    }

}
