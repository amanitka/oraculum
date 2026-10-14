package com.oraculum.company.api.dto;

import java.time.LocalDate;

public record SecFilerDto(
        String cik,
        String managerName,
        short tier,
        boolean active,
        LocalDate lastFilingDate
) {
    public boolean isTier1() {
        return tier == 1;
    }
}
