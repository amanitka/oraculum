package com.oraculum.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_income_statement")
@NoArgsConstructor
public class IncomeStatementEntity extends BaseFinancialStatementEntity {
}