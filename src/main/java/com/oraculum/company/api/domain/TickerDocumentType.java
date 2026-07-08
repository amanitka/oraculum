package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum TickerDocumentType {
    FORM_8K("8K"),
    FORM_10K("10K"),
    EARNINGS_CALL("EARNINGS_CALL");

    private final String code;

    TickerDocumentType(String code) {
        this.code = code;
    }

}
