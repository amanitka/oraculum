package com.oraculum.company.domain;

import com.oraculum.company.api.domain.StatementVariant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndustryFinancialRatiosId implements Serializable {
    private String industryName;
    private StatementVariant variant;
}
